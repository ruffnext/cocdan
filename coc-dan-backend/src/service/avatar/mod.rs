use axum::{Router, routing::{get, post}};

mod crud;

use coc_dan_common::def::avatar::IAvatar;
pub use crud::clear_user_stage_avatars;

use crate::{AppState, entities::avatar};

impl From<avatar::Model> for IAvatar {
    fn from(value: avatar::Model) -> Self {
        Self { 
            id: value.id, 
            stage_uuid: value.stage_uuid, 
            owner: value.owner, 
            name: value.name, 
            detail: serde_json::from_str(value.detail.as_str()).unwrap_or_default()
        }
    }
}

pub fn route() -> Router<AppState> {
    Router::new()
        .route("/:id",          get     (crud::get_by_id_req).
                                                    delete  (crud::destroy))
        .route("/list_owned",   get     (crud::list_by_user))
        .route("/new",          post    (crud::create))
}

#[cfg(test)]
mod test;