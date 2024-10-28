use axum::response::IntoResponse;
use axum::{extract::State, response::Response};

use crate::daemon::DbEntity;
use crate::{daemon::entities::Session, typedef::err::Left, AppState};

pub async fn logout(session: Session, State(state): State<AppState>) -> Result<Response, Left> {
    Session::db_del(session.db_id(), &state.db.manager).await?;
    Ok(("Logout success").into_response())
}
