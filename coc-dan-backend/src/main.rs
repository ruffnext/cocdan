pub mod entities;
mod service;
pub mod state;
pub mod err;

use sea_orm::DatabaseConnection;
use tracing::debug;

use crate::state::get_db;

#[derive(Clone)]
pub struct AppState {
    pub db : DatabaseConnection
}

#[tokio::main]
async fn main() {
    dotenv::dotenv().ok();
    tracing_subscriber::fmt::init();
    let listener = tokio::net::TcpListener::bind("127.0.0.1:3000").await.unwrap();
    let db = get_db().await;
    service::transaction::realtime_tx::initialize();
    debug!("listening on {}", listener.local_addr().unwrap());
    axum::serve(listener, service::app().with_state(AppState{ db })).await.unwrap()
}
