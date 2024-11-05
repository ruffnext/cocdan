use axum::{extract::State, Json};
use serde::Deserialize;
use serde_json::json;

use crate::{
    daemon::{
        entities::{Avatar, Session, SessionType},
        DbEntity,
    },
    left_span,
    typedef::err::{ErrCode, Left},
    AppState,
};

#[derive(Deserialize, ts_rs::TS)]
#[ts(
    export,
    rename = "IReqDeleteAvatar",
    export_to = "api/avatar/delete/IReqDeleteAvatar.d.ts"
)]
pub struct ReqDeleteAvatar {
    raw_id: String,
}

pub async fn delete_avatar(
    State(state): State<AppState>,
    session: Session,
    Json(req): Json<ReqDeleteAvatar>,
) -> Result<Json<serde_json::Value>, Left> {
    let avatar =
        if let Some(v) = Avatar::db_load_by_id(req.raw_id.clone(), &state.db.manager).await? {
            v
        } else {
            return Err(left_span!(ErrCode::PermissionDenied(
                format!("You have no access to delete avatar {}", req.raw_id).into()
            )));
        };

    match session.session_type {
        SessionType::User(user) if user.raw_id == avatar.owner.raw_id => {}
        _ => {
            return Err(left_span!(ErrCode::PermissionDenied(
                format!("You have no access to delete avatar {}", avatar.raw_id).into()
            )));
        }
    }

    Avatar::db_del(avatar.raw_id, &state.db.manager).await?;

    Ok(Json(json!( {
        "message": "Avatar deleted",
    })))
}
