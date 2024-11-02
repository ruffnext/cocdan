use axum::{extract::State, Json};
use surrealdb::sql::Id;

use crate::{
    daemon::{
        entities::{Avatar, AvatarDetail, Session, SessionType, Stage},
        DbEntity,
    },
    left_span,
    typedef::err::{ErrCode, Left},
    AppState,
};

#[derive(serde::Deserialize, ts_rs::TS)]
#[ts(
    export,
    rename = "IReqCreateAvatar",
    export_to = "api/avatar/create/IReqCreateAvatar.d.ts"
)]
pub struct ReqCreateAvatar {
    stage_id: i64,
    name: String,
    #[serde(default)]
    detail: AvatarDetail,
    header: Option<String>,
}

pub async fn create_avatar(
    State(state): State<AppState>,
    session: Session,
    Json(req): Json<ReqCreateAvatar>,
) -> Result<Json<Avatar>, Left> {
    let stage =
        if let Some(v) = Stage::db_load_by_id(Id::from(req.stage_id), &state.db.manager).await? {
            v
        } else {
            return Err(left_span!(ErrCode::InvalidParameter("stage_id".into())));
        };

    if !session.is_on_stage(&stage, &state.db.manager).await {
        return Err(left_span!(ErrCode::PermissionDenied(
            "You have no access to create avatar on this stage".into()
        )));
    }

    let owner = match session.session_type {
        SessionType::User(ref u) => u,
    };

    let new_avatar_id = uuid::Uuid::new_v4();
    let new_avatar = Avatar {
        raw_id: new_avatar_id.to_string(),
        name: req.name,
        detail: req.detail,
        stage: stage.clone(),
        owner: owner.clone(),
        header: req.header,
    };

    new_avatar.db_save(&state.db.manager).await?;

    return Ok(Json(new_avatar));
}
