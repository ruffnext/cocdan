use axum::Router;

use crate::AppState;

// pub mod avatar;
// mod db_relation;
// pub mod stage;
// pub mod transaction;
pub mod user;

pub fn app() -> Router<AppState> {
    Router::new().nest(
        "/api",
        Router::new().nest("/user", user::route()), // .nest("/stage", stage::route())
                                                    // .nest("/avatar", avatar::route()),
    )
}

#[cfg(test)]
pub(crate) mod tests {
    use crate::daemon::{new_mock_db, DbService};

    use super::app;
    use axum_test::{TestResponse, TestServer};

    pub async fn new_test_server() -> (TestServer, DbService) {
        dotenvy::dotenv().ok();
        let db = new_mock_db().await;
        let state = crate::AppState { db: db.clone() };
        (TestServer::new(app().with_state(state)).unwrap(), db)
    }

    pub fn test_extract_left_code<'a>(val: &'a TestResponse) -> String {
        val.json::<serde_json::Value>()["code"]
            .as_str()
            .unwrap_or_default()
            .to_string()
    }
}
