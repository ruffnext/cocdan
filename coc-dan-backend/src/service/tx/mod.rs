mod role_play;
mod state;
use axum::{
    extract::{Path, State, WebSocketUpgrade},
    response::Response,
    routing::{get, post},
    Router,
};

use crate::{
    daemon::{
        entities::{Session, Stage},
        DbEntity,
    },
    left_span,
    typedef::err::{ErrCode, Left},
    AppState,
};

async fn ws_handler(
    ws: WebSocketUpgrade,
    State(state): State<AppState>,
    session: Session,
    Path(stage_id): Path<String>,
) -> Result<Response, Left> {
    let stage = if let Ok(v) = stage_id.parse::<i64>() {
        if let Some(stage) = Stage::db_load_by_id(v, &state.db.manager).await? {
            stage
        } else {
            return Err(left_span!(ErrCode::InvalidParameter(
                "Invalid stage id".into()
            )));
        }
    } else {
        return Err(left_span!(ErrCode::InvalidParameter(
            "Invalid stage id".into()
        )));
    };
    Ok(ws.on_upgrade(move |socket| state.ws.handle_socket(socket, session, stage)))
}

pub fn router() -> Router<AppState> {
    return Router::new()
        .route("/:stage_id/role_play", post(role_play::role_play))
        .route("/:stage_id/ws", get(ws_handler));
}
