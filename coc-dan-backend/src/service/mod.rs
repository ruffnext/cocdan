use axum::Router;

use crate::AppState;

mod avatar;
// mod db_relation;
mod stage;
mod tx;
mod user;

pub fn app() -> Router<AppState> {
    Router::new().nest(
        "/api",
        Router::new()
            .nest("/user", user::route())
            .nest("/stage", stage::route())
            .nest("/avatar", avatar::route())
            .nest("/tx", tx::router()),
    )
}

#[cfg(test)]
#[cfg(feature = "mock")]
pub(crate) mod tests {
    use crate::daemon::DbService;

    use super::app;
    use axum_test::{TestResponse, TestServer};

    pub async fn new_test_server() -> (TestServer, DbService) {
        dotenvy::dotenv().ok();
        let db = DbService::new().await.unwrap();
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
