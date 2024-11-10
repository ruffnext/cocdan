use crate::{
    daemon::{
        entities::{Session, SessionType, Stage},
        relations::RelUserToStage,
        DbEntity, DbRelation,
    },
    left_span,
    typedef::err::{ErrCode, Left},
    AppState,
};
use axum::{
    extract::{Path, State},
    response::{IntoResponse, Response},
    Json,
};
use serde_json::json;

pub async fn join_stage(
    State(state): State<AppState>,
    Path(stage_id): Path<String>,
    session: Session,
) -> Result<Response, Left> {
    let stage = if let Some(v) = Stage::db_load_by_id(stage_id.clone(), &state.db.manager).await? {
        v
    } else {
        return Err(left_span!(ErrCode::InvalidParameter(
            format!("Stage not found: {}", stage_id).into()
        )));
    };

    let user = match session.session_type {
        SessionType::User(u) => u,
    };

    RelUserToStage::rel_save(&user, &stage, &(), &state.db.manager).await?;

    Ok(Json(json!({
        "message": "Joined stage",
    }))
    .into_response())
}

#[derive(Debug, serde::Serialize, serde::Deserialize, ts_rs::TS)]
#[ts(
    export,
    rename = "IRespIsJoinedStage",
    export_to = "api/stage/is_joined/IRespIsJoinedStage.d.ts"
)]
pub struct RespIsJoinedStage {
    pub is_joined: bool,
}

pub async fn is_joined_stage(
    State(state): State<AppState>,
    Path(stage_id): Path<String>,
    session: Session,
) -> Result<Json<RespIsJoinedStage>, Left> {
    let stage = if let Some(v) = Stage::db_load_by_id(stage_id.clone(), &state.db.manager).await? {
        v
    } else {
        return Err(left_span!(ErrCode::InvalidParameter(
            format!("Stage not found: {}", stage_id).into()
        )));
    };

    let user = match session.session_type {
        SessionType::User(u) => u,
    };

    let is_joined = RelUserToStage::rel_existing(&user, &stage, &(), &state.db.manager).await;

    Ok(Json(RespIsJoinedStage { is_joined }))
}
