use axum::{
    routing::{get, post},
    Router, async_trait, extract::{FromRequestParts, FromRef, State, Path}, response::{Response, IntoResponse}
};
use axum_extra::extract::CookieJar;
use coc_dan_common::def::stage::IStage;
use http::request::Parts;
use sea_orm::{EntityTrait, QueryFilter, ColumnTrait};
use uuid::Uuid;

use crate::{AppState, entities::{stage, user, link_stage_user}, service::stage::crud::query_stage_by_id, err::Left};

use super::user::get_session_user;

pub mod crud;

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

pub struct StageUser {
    pub user : user::Model,
    pub stage : IStage
}

#[async_trait]
impl<S> FromRequestParts<S> for StageUser
where S: Send + Sync,
    AppState: FromRef<S>
{
    type Rejection = Response;

    async fn from_request_parts(req: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        let stage_uuid : Path<Uuid> = Path::from_request_parts(req, state).await.map_err(|x| x.into_response())?;
        let cookies = CookieJar::from_request_parts(req, state).await.unwrap();
        let state = AppState::from_ref(state);
        let u = get_session_user(&cookies, State(state.clone())).await.map_err(|x| x.into_response())?;
        let link = link_stage_user::Entity::find()
            .filter(link_stage_user::Column::StageId.eq(stage_uuid.0.to_string()))
            .filter(link_stage_user::Column::UserId.eq(u.id))
            .one(&state.db).await.map_err(|x| Left::from(x).into_response())?;
        if link.is_none() {
            Err(Left {
                status : http::StatusCode::UNAUTHORIZED,
                message : "Please join stage first".to_string(),
                uuid : "2b46d6bb"
            }.into_response())?
        }
        let s = query_stage_by_id(&u, stage_uuid.0, &state.db).await.map_err(|x| x.into_response())?;
        Ok(StageUser {
            user : u,
            stage : s
        })
    }
}

pub fn route() -> Router<AppState> {
    Router::new()
        .route("/:uuid",        get (crud::get_by_uuid))
        .route("/:uuid/users",  get (crud::list_users_by_stage))
        .route("/:uuid/join",   post(crud::join_stage))
        .route("/:uuid/leave",  post(crud::leave_stage))
        .route("/:uuid/txs",    get(super::transaction::crud::query_stage_txs))
        .route("/new",          post(crud::create))
        .route("/my_stages",    get (crud::list_stages_by_user))
}

#[cfg(test)]
mod test;
