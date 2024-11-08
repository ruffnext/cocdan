use std::collections::HashMap;

use axum::{
    extract::{Path, State},
    Json,
};
use serde::{Deserialize, Serialize};

use crate::{
    daemon::{
        entities::{AvatarDbAux, AvatarDetail, Session, Stage, TxEvent},
        DbEntity,
    },
    left_span,
    typedef::err::{ErrCode, Left},
    AppState,
};

#[derive(Deserialize, Serialize, ts_rs::TS)]
#[ts(
    export,
    export_to = "api/tx/state/IGameState.d.ts",
    rename = "IGameState"
)]
pub struct GameState {
    pub avatars: HashMap<String, AvatarDetail>,
    pub logs: Vec<TxEvent>,
}

#[derive(Deserialize, ts_rs::TS)]
#[ts(
    export,
    export_to = "api/tx/state/IReqFetchGameState.d.ts",
    rename = "IReqFetchGameState"
)]
pub struct ReqFetchGameState {
    pub n_page: u32,
    pub n_per_page: u32,
}

pub async fn fetch_game_state(
    State(state): State<AppState>,
    Path(stage_id): Path<String>,
    session: Session,
    Json(req): Json<ReqFetchGameState>,
) -> Result<Json<GameState>, Left> {
    let stage =
        if let Some(stage) = Stage::db_load_by_id(stage_id.clone(), &state.db.manager).await? {
            stage
        } else {
            return Err(left_span!(ErrCode::InvalidParameter(
                "Invalid stage id".into()
            )));
        };

    if !session.is_on_stage(stage_id, &state.db.manager).await {
        return Err(left_span!(ErrCode::PermissionDenied(
            "You are not on this stage".into()
        )));
    }

    let query = format!(
        "
        SELECT 
            *,
            avatar.raw_id as avatar_id,
            stage.raw_id as stage_id,
            user.raw_id as user_id
        FROM tx WHERE stage = {stage} ORDER BY time DESC LIMIT {n_per_page} START {n_page};
    ",
        stage = stage.db_thing().to_string(),
        n_per_page = req.n_per_page,
        n_page = req.n_page
    );

    let logs: Vec<TxEvent> = state.db.query_manager(&query).await?;

    let first_log_time = if let Some(log) = logs.last() {
        log.time.clone()
    } else {
        return Ok(Json(GameState {
            avatars: HashMap::new(),
            logs,
        }));
    };

    let query_avatars = format!(
        "SELECT * FROM avatar WHERE stage = {stage} VERSION d'{first_log_time}';",
        stage = stage.db_thing().to_string()
    );

    let avatars: Vec<AvatarDbAux> = state.db.query_manager(&query_avatars).await?;

    let avatars = avatars
        .into_iter()
        .map(|avatar| (avatar.raw_id.clone(), avatar.detail))
        .collect();

    Ok(Json(GameState { avatars, logs }))
}
