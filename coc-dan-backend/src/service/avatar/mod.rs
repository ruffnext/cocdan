use axum::{Router, routing::{get, post}, async_trait, extract::{FromRequestParts, FromRef, Path}, response::{Response, IntoResponse}};

pub mod crud;

use coc_dan_common::def::avatar::IAvatar;
pub use crud::clear_user_stage_avatars;
use http::request::Parts;
use sea_orm::EntityTrait;

use crate::{AppState, entities::{avatar, user}, err::Left};

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

pub struct UserAvatar {
    pub user : user::Model,
    pub avatar : avatar::Model
}

#[async_trait]
impl<S> FromRequestParts<S> for UserAvatar
where S: Send + Sync,
    AppState: FromRef<S>
{
    type Rejection = Response;

    async fn from_request_parts(req: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        let avatar_id : Path<i32> = Path::from_request_parts(req, state).await.map_err(|x| x.into_response())?;
        let u : user::Model = user::Model::from_request_parts(req, state).await?;
        let state = AppState::from_ref(state);
        let a = avatar::Entity::find_by_id(avatar_id.0).one(&state.db).await.map_err(|x| Left::from(x).into_response())?;
        match a {
            Some(v) if v.owner == u.id => {
                Ok(UserAvatar {
                    user : u,
                    avatar : v
                })
            },
            _ => {
                Err(Left {
                    status : http::StatusCode::BAD_REQUEST,
                    message : format!("Avatar {} not found", avatar_id.0),
                    uuid : ""
                }.into_response())
            }
        }
    }
}

pub fn route() -> Router<AppState> {
    Router::new()
        .route("/:id",              get     (crud::get_by_id_req).
                                                        delete  (crud::destroy))
        .route("/:id/transaction",  post(super::transaction::crud::action))
        .route("/list_owned",       get     (crud::list_by_user))
        .route("/new",              post    (crud::create))
}

#[cfg(test)]
mod test;
