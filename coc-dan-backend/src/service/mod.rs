use axum::{response::IntoResponse, Json, Router};

use crate::{err::Left, AppState};

pub mod avatar;
mod db_relation;
pub mod stage;
pub mod transaction;
pub mod user;

impl IntoResponse for Left {
    fn into_response(self) -> axum::response::Response {
        if self.status == http::StatusCode::NO_CONTENT {
            self.status.into_response()
        } else {
            (self.status, Json(self)).into_response()
        }
    }
}

pub fn app() -> Router<AppState> {
    Router::new().nest(
        "/api",
        Router::new()
            .nest("/user", user::route())
            .nest("/stage", stage::route())
            .nest("/avatar", avatar::route()),
    )
}

#[cfg(test)]
pub(crate) mod tests {
    use super::app;
    use crate::state::tests::new_mock_db;
    use axum_test::{TestResponse, TestServer};
    use sea_orm::DatabaseConnection;

    pub async fn new_test_server() -> (TestServer, DatabaseConnection) {
        let db = new_mock_db().await;
        let state = crate::AppState { db: db.clone() };
        (TestServer::new(app().with_state(state)).unwrap(), db)
    }

    pub fn test_extract_left_uuid<'a>(val: &'a TestResponse) -> String {
        val.json::<serde_json::Value>()["uuid"]
            .as_str()
            .unwrap()
            .to_string()
    }
}
