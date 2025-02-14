use axum::{routing::post, Router};
mod create;
mod delete;
mod get;
mod update;
// pub mod crud;

use crate::AppState;

pub fn route() -> Router<AppState> {
    Router::new()
        .route("/new", post(create::create_avatar))
        .route("/update", post(update::update_avatar))
        .route("/delete", post(delete::delete_avatar))
        .route("/get", post(get::get_avatar_by_id))
    // .route("/:id", get(crud::get_by_id_req).delete(crud::destroy))
    // .route(
    //     "/:id/transaction",
    //     post(super::transaction::crud::action_service),
    // )
    // .route("/:id/update", post(crud::update_avatar))
    // .route("/list_owned", get(crud::list_by_user))
    // .route("/new", post(crud::create))
}
