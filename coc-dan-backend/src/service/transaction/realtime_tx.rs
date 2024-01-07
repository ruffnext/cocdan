
use std::collections::HashMap;

use coc_dan_common::def::transaction::ITransaction;
use sea_orm::{EntityTrait, QueryFilter, ColumnTrait, ConnectionTrait, QueryOrder};
use tokio::sync::{OnceCell, RwLock};

use crate::{err::Left, entities::{stage, avatar, transaction}};

pub use coc_dan_common::def::state::State as RealtimeState;

static REALTIME_STATE : OnceCell<RwLock<HashMap<i32, RealtimeState>>> = OnceCell::const_new();

async fn init() -> RwLock<HashMap<i32, RealtimeState>> {
    return RwLock::new(HashMap::new())
}

pub async fn get_realtime_state() -> &'static RwLock<HashMap<i32, RealtimeState>> {
    return REALTIME_STATE.get_or_init(init).await;
}

pub async fn get_state_by_stage_id <T : ConnectionTrait> (stage_id : i32, db : &T) -> Result<RealtimeState, Left> {
    {
        let x = get_realtime_state().await.read().await;
        match x.get(&stage_id) {
            Some(v) => { return Ok(v.clone()) },
            None => {}
        }
    }
    {
        let mut dict = get_realtime_state().await.write().await;
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
                    message : format!("Stage {stage_id} has not transaction!"),
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
        dict.insert(stage_id, state.clone());
        return Ok(state)
    }
}

pub async fn step_state (stage_id : i32, mut state : RealtimeState, tx : &ITransaction) {
    state.apply_tx(tx);

    let mut dict = get_realtime_state().await.write().await;

    dict.insert(stage_id, state);
}
