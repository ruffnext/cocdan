use std::{sync::Arc, time::Duration};

use axum::extract::ws::{Message, WebSocket};
use futures::{stream::SplitSink, SinkExt, StreamExt};
use tokio::{
    sync::{Mutex, RwLock},
    time::timeout,
};
use uuid::Uuid;

use crate::daemon::{
    entities::{Session, Stage},
    DbEntity,
};

use super::{StageTx, StageTxSubscriber, WsServer};

#[derive(serde::Serialize, serde::Deserialize, ts_rs::TS)]
#[ts(
    export,
    export_to = "api/ws/tx/IReqJoinStage.d.ts",
    rename = "IReqJoinStage"
)]
pub struct ReqJoinStage {
    pub session: String,
    pub stage_id: i64,
}

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
    pub async fn handle_socket(self, ws: WebSocket) {
        let (mut ws_sender, mut ws_receiver) = ws.split();

        let (session, stage) = if let Ok(Some(Ok(Message::Text(text)))) =
            timeout(Duration::from_secs(10), ws_receiver.next()).await
        {
            if let Ok(req) = serde_json::from_str::<ReqJoinStage>(&text) {
                let session = if let Ok(Some(v)) =
                    Session::db_load_by_id(req.session, &self.db.manager).await
                {
                    v
                } else {
                    RespJoinStage::Fail("Invalid session".into())
                        .send(&mut ws_sender)
                        .await
                        .ok();
                    return;
                };

                let stage = match Stage::db_load_by_id(req.stage_id, &self.db.manager).await {
                    Ok(Some(stage)) => stage,
                    _ => {
                        RespJoinStage::Fail("Invalid stage".into())
                            .send(&mut ws_sender)
                            .await
                            .ok();
                        return;
                    }
                };

                (session, stage)
            } else {
                RespJoinStage::Fail("Invalid request".into())
                    .send(&mut ws_sender)
                    .await
                    .ok();
                return;
            }
        } else {
            RespJoinStage::Fail("Invalid request".into())
                .send(&mut ws_sender)
                .await
                .ok();
            return;
        };

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
