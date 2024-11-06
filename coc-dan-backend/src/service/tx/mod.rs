mod role_play;
mod state;
use axum::{
    extract::{State, WebSocketUpgrade},
    response::Response,
    routing::{get, post},
    Router,
};

use crate::AppState;

async fn ws_handler(ws: WebSocketUpgrade, State(state): State<AppState>) -> Response {
    ws.on_upgrade(move |socket| state.ws.handle_socket(socket))
}

pub fn router() -> Router<AppState> {
    return Router::new()
        .route("/:stage_id/role_play", post(role_play::role_play))
        .route("/ws", get(ws_handler));
}
