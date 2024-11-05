use std::{collections::HashMap, sync::Arc};

use axum::extract::ws::{Message, WebSocket};
use futures::stream::SplitSink;
use tokio::sync::Mutex;
mod handler;

use super::{db::DbConn, entities::Session};

#[derive(Clone)]
pub struct WsServer {
    stage_tx: Arc<Mutex<HashMap<i64, StageTx>>>,
}

#[derive(Clone)]
struct StageTx {
    stage_id: i64,
    subscribers: Arc<Mutex<Vec<StageTxSubscriber>>>,
}

struct StageTxSubscriber {
    session: Session,
    sender: SplitSink<WebSocket, Message>,
}

impl WsServer {
    pub async fn new(db: DbConn) -> Self {
        Self {
            stage_tx: Arc::new(Mutex::new(HashMap::new())),
        }
    }
}
