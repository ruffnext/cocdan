use std::{collections::HashMap, u32};

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
    export_to = "api/tx/state/IGameStateFragment.d.ts",
    rename = "IGameStateFragment"
)]
pub struct GameStateFragment {
    pub begin_tx_index: u32,
    pub end_tx_index: u32,
    pub stage_id: String,
    pub avatars: HashMap<String, AvatarDetail>,
    pub logs: Vec<TxEvent>,
}

impl Default for GameStateFragment {
    fn default() -> Self {
        Self {
            begin_tx_index: 0,
            end_tx_index: 0,
            stage_id: "".into(),
            avatars: HashMap::new(),
            logs: Vec::new(),
        }
    }
}

#[derive(Deserialize, ts_rs::TS)]
#[ts(
    export,
    export_to = "api/tx/state/IReqFetchGameStateFragment.d.ts",
    rename = "IReqFetchGameStateFragment"
)]
pub struct ReqFetchGameState {
    /// end of the transaction id (exclusive)
    pub end_tx_index: Option<u32>,

    /// number of logs
    pub count: u32,
}

pub async fn fetch_game_state(
    State(state): State<AppState>,
    Path(stage_id): Path<String>,
    session: Session,
    Json(req): Json<ReqFetchGameState>,
) -> Result<Json<GameStateFragment>, Left> {
    let stage =
        if let Some(stage) = Stage::db_load_by_id(stage_id.clone(), &state.db.manager).await? {
            stage
        } else {
            return Err(left_span!(ErrCode::InvalidParameter(
                "Invalid stage id".into()
            )));
        };

    if !session
        .is_on_stage(stage_id.clone(), &state.db.manager)
        .await
    {
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
        FROM tx WHERE stage = {stage} AND tx_index < {tx_index} ORDER BY time DESC LIMIT {count};
    ",
        stage = stage.db_thing().to_string(),
        count = req.count,
        tx_index = req.end_tx_index.unwrap_or(u32::MAX),
    );

    let logs: Vec<TxEvent> = state.db.query_manager(&query).await?;

    let first_log = if let Some(log) = logs.last() {
        log.clone()
    } else {
        return Ok(Json(GameStateFragment::default()));
    };

    let last_log = if let Some(log) = logs.first() {
        log.clone()
    } else {
        return Ok(Json(GameStateFragment::default()));
    };

    let query_avatars = format!(
        "(  SELECT VALUE
                array::first(SELECT 
                    version,
                    tx.avatar.raw_id as raw_id,
                    tx.stage as stage,
                    tx.avatar.owner as owner,
                    tx.avatar.creation_time as creation_time
                FROM fn::load_version($parent.id, {first_log_time})) AS version
            FROM avatar WHERE stage == {stage}
            FETCH version
        ).filter(|$x| $x.version != None);
        ",
        first_log_time = first_log.time.to_string(),
        stage = stage.db_thing().to_string()
    );

    let avatars: Vec<AvatarDbAux> = state.db.query_manager(&query_avatars).await?;

    let avatars = avatars
        .into_iter()
        .map(|avatar| (avatar.raw_id.clone(), avatar.version))
        .collect();

    Ok(Json(GameStateFragment {
        begin_tx_index: first_log.tx_index as u32,
        end_tx_index: last_log.tx_index as u32,
        stage_id: stage_id.clone(),
        avatars,
        logs,
    }))
}
