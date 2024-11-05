use std::sync::Arc;

use axum::extract::ws::WebSocket;
use futures::StreamExt;
use tokio::sync::Mutex;

use super::WsServer;

impl WsServer {
    pub async fn handle_socket(&self, ws: WebSocket) {
        let (ws_sender, ws_receiver) = ws.split();
        let ws_sender = Arc::new(Mutex::new(ws_sender));
        let ws_receiver = Arc::new(Mutex::new(ws_receiver));

        let (tx, rx) = tokio::sync::mpsc::channel::<String>(100);
    }
}
