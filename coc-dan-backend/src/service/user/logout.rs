use axum::response::IntoResponse;
use axum::Json;
use axum::{extract::State, response::Response};
use serde_json::json;

use crate::daemon::DbEntity;
use crate::{daemon::entities::Session, typedef::err::Left, AppState};

pub async fn logout(session: Session, State(state): State<AppState>) -> Result<Response, Left> {
    Session::db_del(session.db_id(), &state.db.manager).await?;
    Ok(Json(json!({
        "message": "logout success"
    }))
    .into_response())
}
