
use std::collections::HashMap;

use coc_dan_common::def::state::State;
use sea_orm::{DatabaseConnection, EntityTrait, QueryFilter, ColumnTrait};
use tokio::sync::{OnceCell, RwLock};
use uuid::Uuid;

use crate::{err::Left, entities::{stage, avatar}};

static mut REALTIME_STATE : OnceCell<RwLock<HashMap<Uuid, State>>> = OnceCell::const_new();

pub fn initialize() {
    unsafe { REALTIME_STATE.set(RwLock::new(HashMap::new())).unwrap() };
}

pub async fn query_realtime_state (stage_uuid : Uuid, db : &DatabaseConnection) -> Result<State, Left> {
    {
        let x = unsafe { REALTIME_STATE.get().unwrap().read().await };
        match x.get(&stage_uuid) {
            Some(v) => { return Ok(v.clone()) },
            None => {}
        }
    }
    {
        let mut dict = unsafe { REALTIME_STATE.get().unwrap().write().await };
        let s = match stage::Entity::find_by_id(stage_uuid).one(db).await? {
            Some(v) => v,
            None => {
                Err(Left {
                    status : http::StatusCode::BAD_REQUEST,
                    message : format!("Stage {stage_uuid} does not exists!"),
                    uuid : "60aa8135"
                })?
            }
        };
        let avatars = avatar::Entity::find()
            .filter(avatar::Column::StageUuid.eq(stage_uuid))
            .all(db).await?;
        let res_state : State = State {
            idx : 0,
            avatars : avatars.into_iter().map(|x| (x.id, x.into())).collect(),
            game_map : serde_json::from_str(&s.game_map).unwrap()
        };
        dict.insert(stage_uuid, res_state.clone());
        return Ok(res_state)
    }
}
