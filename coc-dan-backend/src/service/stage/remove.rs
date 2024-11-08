use axum::{
    extract::{Path, State},
    response::{IntoResponse, Response},
    Json,
};
use serde_json::json;

use crate::{
    daemon::{
        entities::{Stage, User},
        DbEntity,
    },
    left_span,
    typedef::err::{ErrCode, Left},
    AppState,
};

pub async fn remove_stage(
    State(state): State<AppState>,
    Path(stage_id): Path<String>,
    user: User,
) -> Result<Response, Left> {
    if let Some(stage) = Stage::db_load_by_id(stage_id.clone(), &state.db.manager).await? {
        if stage.owner.raw_id != user.raw_id {
            return Err(left_span!(ErrCode::PermissionDenied(
                "You are not the owner of this stage".into()
            )));
        }
        Stage::db_del(stage_id, &state.db.manager).await?;
        Ok(Json(json! {
            {
                "message" : "Stage removed"
            }
        })
        .into_response())
    } else {
        return Err(left_span!(ErrCode::InvalidParameter(
            "Stage not found".into()
        )));
    }
}
