mod role_play;
use axum::{routing::post, Router};

use crate::AppState;

pub fn router() -> Router<AppState> {
    return Router::new().route("/:stage_id/role_play", post(role_play::role_play));
}
