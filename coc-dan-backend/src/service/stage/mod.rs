use crate::AppState;
use axum::{
    routing::{get, post},
    Router,
};
mod create;
mod get;
mod remove;

pub fn route() -> Router<AppState> {
    Router::new()
        .route("/:id", get(get::get_stage))
        // .route("/:id/users", get(crud::list_users_by_stage))
        // .route("/:id/join", post(crud::join_stage))
        // .route("/:id/leave", post(crud::leave_stage))
        // .route("/:id/txs", get(super::transaction::crud::query_stage_txs))
        // .route(
        //     "/:id/state",
        //     get(super::transaction::crud::query_stage_realtime_state),
        // )
        .route("/new", post(create::create_stage))
        .route("/:stage_id/remove", post(remove::remove_stage))
    // .route("/my_stages", get(crud::list_stages_by_user))
}

// #[cfg(test)]
// mod test;
