use std::{collections::HashMap, sync::Arc};

use axum::extract::ws::{Message, WebSocket};
use futures::stream::SplitSink;
use tokio::sync::{Mutex, RwLock};
mod handler;
mod tx_event;

use super::{entities::Session, DbService};

#[derive(Clone)]
pub struct WsServer {
    stage_tx: Arc<RwLock<HashMap<i64, StageTx>>>,
    db: DbService,
}

#[derive(Clone)]
struct StageTx {
    #[allow(unused)]
    stage_id: i64,
    subscribers: Arc<RwLock<Vec<(String, Arc<Mutex<StageTxSubscriber>>)>>>,
}

struct StageTxSubscriber {
    #[allow(unused)]
    session: Session,
    sender: SplitSink<WebSocket, Message>,
    is_error: bool,
}

impl WsServer {
    pub async fn new(db: DbService) -> Self {
        let res = Self {
            stage_tx: Arc::new(RwLock::new(HashMap::new())),
            db,
        };

        let copied = res.clone();
        tokio::spawn(async move {
            copied.live_tx().await;
        });

        res
    }
}
