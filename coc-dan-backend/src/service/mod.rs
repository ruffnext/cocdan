use axum::{Router, response::IntoResponse, Json};

use crate::{AppState, err::Left};

pub mod user;
pub mod stage;
pub mod avatar;
pub mod transaction;
mod db_relation;

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
    Router::new()
        .nest("/api", Router::new()
            .nest("/user", user::route())
            .nest("/stage", stage::route())
            .nest("/avatar", avatar::route()))
}

#[cfg(test)]
pub (crate) mod tests {
    use crate::state::tests::new_mock_db;
    use super::app;
    use axum_test::{TestServer, TestResponse};
    use sea_orm::DatabaseConnection;

    pub async fn new_test_server() -> (TestServer, DatabaseConnection) {
        let db = new_mock_db().await;
        let state = crate::AppState { db : db.clone() };
        (TestServer::new(app().with_state(state)).unwrap(), db)
    }
    
    pub fn test_extract_left_uuid<'a>(val : &'a TestResponse) -> String {
        val.json::<serde_json::Value>()["uuid"].as_str().unwrap().to_string()
    }
}