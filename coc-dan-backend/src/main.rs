pub(self) mod daemon;
mod service;
mod state;
mod typedef;
mod utils;

use daemon::{DbService, WsServer};
use tracing::info;

// use crate::state::get_db;

#[derive(Clone)]
pub struct AppState {
    pub db: DbService,
    pub ws: WsServer,
}

#[tokio::main]
async fn main() {
    dotenvy::dotenv().ok();
    tracing_subscriber::fmt::init();
    let db = DbService::new().await.expect("failed to connect to db");
    let ws = WsServer::new(db.clone()).await;
    let server_bind = std::env::var("SERVER_BIND").unwrap_or("127.0.0.1:3000".to_string());
    let listener = tokio::net::TcpListener::bind(server_bind.clone())
        .await
        .unwrap();
    info!("listening on {server_bind}");
    axum::serve(listener, service::app().with_state(AppState { db, ws }))
        .await
        .unwrap()
}
