
use std::collections::HashMap;

use coc_dan_common::def::transaction::{ITransaction, Tx};
use sea_orm::{EntityTrait, QueryFilter, ColumnTrait, ConnectionTrait, QueryOrder, ActiveValue, ActiveModelTrait};
use tokio::sync::{OnceCell, RwLock, RwLockReadGuard, RwLockWriteGuard};

use crate::{err::Left, entities::{stage, avatar, transaction}};

pub use coc_dan_common::def::state::State as RealtimeState;

static STAGE_STATE : OnceCell<RwLock<HashMap<i32, RwLock<RealtimeState>>>> = OnceCell::const_new();

async fn init_state() -> RwLock<HashMap<i32, RwLock<RealtimeState>>> {
    return RwLock::new(HashMap::new())
}

pub async fn get_realtime_state_write_lock() -> RwLockWriteGuard<'static, HashMap<i32, RwLock<RealtimeState>>>  {
    STAGE_STATE.get_or_init(init_state).await.write().await
}

pub async fn query_stage_realtime_state_in_db <T> (
    stage_id : i32,
    db : &T
) -> Result<RealtimeState, Left>
where T : ConnectionTrait
{
    let s = match stage::Entity::find_by_id(stage_id).one(db).await? {
        Some(v) => v,
        None => {
            Err(Left {
                status : http::StatusCode::BAD_REQUEST,
                message : format!("Stage {stage_id} does not exists!"),
                uuid : "60aa8135"
            })?
        }
    };
    let last_tx = match transaction::Entity::find()
        .filter(transaction::Column::StageId.eq(stage_id))
        .order_by_desc(transaction::Column::Id)
        .one(db).await? {
        Some(v) => v,
        None => {
            Err(Left {
                status : http::StatusCode::INTERNAL_SERVER_ERROR,
                message : format!("Stage {stage_id} has no transaction!"),
                uuid : "c8715828"
            })?
        }
    };
    let avatars = avatar::Entity::find()
        .filter(avatar::Column::StageId.eq(stage_id))
        .all(db).await?;
    let state = RealtimeState {
        last_tx : last_tx.into(),
        avatars : avatars.into_iter().map(|x| (x.id, x.into())).collect(),
        game_map : serde_json::from_str(&s.game_map).unwrap()
    };
    Ok(state)
}


pub type StageStateLock = RwLockReadGuard<'static, HashMap<i32, RwLock<RealtimeState>>>;


pub async fn lock_stage_state <T> (
    stage_id : i32,
    db : &T
) -> Result<StageStateLock, Left> 
where T: ConnectionTrait {
    let state : &'static RwLock<HashMap<i32, RwLock<RealtimeState>>> = STAGE_STATE.get_or_init(init_state).await;
    let global_lock = state.read().await;
    let global_read_lock : RwLockReadGuard<'static, HashMap<i32, RwLock<RealtimeState>>> = if global_lock.contains_key(&stage_id) == false {
        drop(global_lock);
        let mut global_write_lock = state.write().await;
        let new_state = query_stage_realtime_state_in_db(stage_id, db).await?;
        global_write_lock.insert(stage_id, RwLock::new(new_state));
        drop(global_write_lock);
        state.read().await
    } else {
        global_lock
    };
    return Ok(global_read_lock)
}

pub async fn perform_tx<T> (
    (state, stage_id) : (&mut RwLockWriteGuard<'_, RealtimeState>, i32),
    db : &T,
    user_id : i32,
    avatar_id : i32,
    tx : &Tx
) -> Result<bool, Left> 
where T : ConnectionTrait {
    let latest_tx_id = state.last_tx.tx_id;
    let res : ITransaction = transaction::ActiveModel {
        tx_id : ActiveValue::Set(latest_tx_id as i32 + 1),
        stage_id : ActiveValue::Set(stage_id),
        user_id : ActiveValue::Set(user_id),
        time : ActiveValue::Set(chrono::Local::now().to_rfc3339()),
        tx : ActiveValue::Set(serde_json::to_string(tx).unwrap()),
        avatar_id : ActiveValue::Set(avatar_id),
        ..Default::default()
    }.insert(db).await?.into();
    Ok(state.apply_tx(&res))
}
