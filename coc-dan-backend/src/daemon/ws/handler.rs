use std::sync::Arc;

use axum::extract::ws::{Message, WebSocket};
use futures::{stream::SplitSink, SinkExt, StreamExt};
use tokio::sync::{Mutex, RwLock};
use uuid::Uuid;

use crate::daemon::entities::{Session, Stage};

use super::{StageTx, StageTxSubscriber, WsServer};

#[derive(serde::Serialize, serde::Deserialize, ts_rs::TS)]
#[ts(
    export,
    export_to = "api/ws/tx/IRespJoinStage.d.ts",
    rename = "IRespJoinStage"
)]
pub enum RespJoinStage {
    Success(String),
    Fail(String),
}

impl RespJoinStage {
    async fn send(&self, sender: &mut SplitSink<WebSocket, Message>) -> Result<(), axum::Error> {
        return sender
            .send(Message::Text(serde_json::to_string(self).unwrap()))
            .await;
    }
}

impl WsServer {
    pub async fn handle_socket(self, ws: WebSocket, session: Session, stage: Stage) {
        let (mut ws_sender, _) = ws.split();

        if let Err(_) = RespJoinStage::Success("Success".into())
            .send(&mut ws_sender)
            .await
        {
            return;
        }

        let ws_id = Uuid::new_v4().to_string();
        let mut stage_tx = self.stage_tx.write().await;
        let entry = stage_tx.entry(stage.raw_id).or_insert(StageTx {
            stage_id: stage.raw_id,
            subscribers: Arc::new(RwLock::new(Vec::new())),
        });
        entry.subscribers.write().await.push((
            ws_id,
            Arc::new(Mutex::new(StageTxSubscriber {
                session,
                sender: ws_sender,
                is_error: false,
            })),
        ));
    }
}
