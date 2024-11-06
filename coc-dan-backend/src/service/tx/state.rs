use crate::daemon::entities::{Avatar, TxEvent};

pub struct GameState {
    pub avatars: Vec<Avatar>,
    pub logs: Vec<TxEvent>,
}

pub async fn fetch_game_state() {}
