use axum::Json;

use crate::daemon::entities::Session;

pub async fn get_me(session: Session) -> Json<Session> {
    Json(session)
}
