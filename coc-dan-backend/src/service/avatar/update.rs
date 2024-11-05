use axum::{extract::State, Json};
use serde::{Deserialize, Serialize};

use crate::{
    daemon::{
        entities::{Avatar, AvatarDetail, Session, SessionType},
        DbEntity,
    },
    left_span,
    typedef::err::{ErrCode, Left},
    AppState,
};

#[derive(Debug, Serialize, Deserialize, ts_rs::TS)]
#[ts(
    export,
    rename = "IReqUpdateAvatar",
    export_to = "api/avatar/update/IReqUpdateAvatar.d.ts"
)]
pub struct ReqUpdateAvatar {
    raw_id: String,
    name: String,
    detail: AvatarDetail,
    header: String,
}

pub async fn update_avatar(
    State(state): State<AppState>,
    session: Session,
    Json(req): Json<ReqUpdateAvatar>,
) -> Result<Json<Avatar>, Left> {
    let avatar =
        if let Some(v) = Avatar::db_load_by_id(req.raw_id.clone(), &state.db.manager).await? {
            v
        } else {
            return Err(left_span!(ErrCode::PermissionDenied(
                format!("You have no access to update avatar {}", req.raw_id).into()
            )));
        };

    match (session.session_type, avatar.stage.owner.clone()) {
        (SessionType::User(u), stage_owner)
            if u.raw_id == avatar.owner.raw_id || u.raw_id == stage_owner.raw_id => {}
        _ => {
            return Err(left_span!(ErrCode::PermissionDenied(
                format!("You have no access to update avatar {}", req.raw_id).into()
            )));
        }
    };

    let new_avatar = Avatar {
        owner: avatar.owner.clone(),
        stage: avatar.stage.clone(),
        name: req.name.clone(),
        ..avatar
    };

    new_avatar.db_save(&state.db.manager).await?;

    Ok(Json(new_avatar))
}
