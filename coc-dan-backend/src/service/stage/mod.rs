use axum::{
    routing::{get, post},
    Router
};
use coc_dan_common::def::stage::IStage;

use crate::{AppState, entities::stage};

mod crud;

impl From<stage::Model> for IStage {
    fn from(value: stage::Model) -> Self {
        Self { 
            uuid: value.uuid, 
            owner: value.owner, 
            title: value.title, 
            description: value.description, 
            area: serde_json::from_str(&value.areas).unwrap() 
        }
    }
}

pub fn route() -> Router<AppState> {
    Router::new()
        .route("/:uuid",        get (crud::get_by_uuid))
        .route("/:uuid/users",  get (crud::list_users_by_stage))
        .route("/:uuid/join",   post(crud::join_stage))
        .route("/:uuid/leave",  post(crud::leave_stage))
        .route("/new",          post(crud::create))
        .route("/my_stages",    get (crud::list_stages_by_user))
}

#[cfg(test)]
mod test;
