use axum::{extract::State, Json};

use crate::{
    daemon::entities::{Avatar, Session},
    left_span,
    typedef::err::{ErrCode, Left},
};

#[derive(serde::Deserialize, ts_rs::TS)]
#[ts(
    export,
    rename = "IReqGetAvatar",
    export_to = "api/avatar/get/IReqGetAvatar.d.ts"
)]
pub struct ReqGetAvatar {
    pub avatar_id: String,
}

pub async fn get_avatar_by_id(
    State(state): State<crate::AppState>,
    _session: Session,
    Json(req): Json<ReqGetAvatar>,
) -> Result<Json<Avatar>, Left> {
    if let Some(v) = Avatar::db_load_by_id(req.avatar_id, &state.db.manager).await? {
        Ok(Json(v))
    } else {
        Err(left_span!(ErrCode::NoContent))
    }
}
