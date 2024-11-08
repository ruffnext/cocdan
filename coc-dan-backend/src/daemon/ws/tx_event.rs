use std::{collections::HashSet, str::FromStr, time::Duration};

use axum::extract::ws::Message;
use futures::{SinkExt, StreamExt};
use tokio::time;
use tracing::{error, info};

use crate::daemon::{
    entities::{TxAux, TxEvent},
    DbEntity,
};

use super::WsServer;

impl TryFrom<&TxAux> for TxEvent {
    type Error = <i64 as FromStr>::Err;
    fn try_from(value: &TxAux) -> Result<Self, Self::Error> {
        Ok(Self {
            raw_id: value.raw_id.clone(),
            tx_index: value.tx_index,
            stage_id: value.stage.id.to_raw(),
            user_id: value.user.id.to_raw(),
            avatar_id: value.avatar.id.to_raw().to_string(),
            time: value.time.to_string(),
            action: value.action.clone(),
        })
    }
}

impl WsServer {
    pub(super) async fn live_tx(self) {
        let mut is_last_error = false;
        loop {
            if is_last_error {
                is_last_error = false;
                time::sleep(Duration::from_secs(1)).await;
            }
            match self.db.manager.select(TxAux::db_tab_name()).live().await {
                Ok(mut stream) => {
                    while let Some(val) = stream.next().await {
                        match &val {
                            Ok(notify_tx) => {
                                let notify: &surrealdb::Notification<TxAux> = notify_tx;
                                let tx: TxEvent = match TxEvent::try_from(&notify.data) {
                                    Ok(v) => v,
                                    Err(e) => {
                                        error!("tx val error {:#?}", e);
                                        is_last_error = true;
                                        continue;
                                    }
                                };
                                let channels = self.stage_tx.read().await;
                                let channel = if let Some(v) = channels.get(&tx.stage_id) {
                                    v.clone()
                                } else {
                                    continue;
                                };

                                let mut error_subscribers = HashSet::new();
                                for (ws_id, subscriber) in channel.subscribers.read().await.iter() {
                                    let subscriber = subscriber.clone();
                                    let is_error = if let Ok(v) = subscriber.try_lock() {
                                        v.is_error
                                    } else {
                                        continue;
                                    };
                                    if is_error {
                                        error_subscribers.insert(ws_id.clone());
                                    } else {
                                        let tx = tx.clone();
                                        tokio::spawn(async move {
                                            let mut lock = subscriber.lock().await;
                                            lock.sender
                                                .send(Message::Text(
                                                    serde_json::to_string(&tx).unwrap(),
                                                ))
                                                .await
                                        });
                                    }
                                    continue;
                                }
                                if !error_subscribers.is_empty() {
                                    let mut lock = channel.subscribers.write().await;
                                    lock.retain(|(ws_id, _)| !error_subscribers.contains(ws_id));
                                }
                            }
                            Err(e) => {
                                info!("tx val error {:#?}", e);
                                is_last_error = true;
                            }
                        }
                    }
                }
                Err(e) => {
                    info!("tx stream error {:#?}", e);
                    is_last_error = true;
                }
            }
        }
    }
}
