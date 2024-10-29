use crate::AppState;
use axum::{routing::post, Router};
mod create;
mod remove;

// use super::user::get_session_user;

// pub mod crud;

// #[async_trait]
// impl<S> FromRequestParts<S> for StageUser
// where
//     S: Send + Sync,
//     AppState: FromRef<S>,
// {
//     type Rejection = Response;

//     async fn from_request_parts(req: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
//         let stage_id: Path<i32> = Path::from_request_parts(req, state)
//             .await
//             .map_err(|x| x.into_response())?;
//         let cookies = CookieJar::from_request_parts(req, state).await.unwrap();
//         let state = AppState::from_ref(state);
//         let u = get_session_user(&cookies, State(state.clone()))
//             .await
//             .map_err(|x| x.into_response())?;
//         let link = link_stage_user::Entity::find()
//             .filter(link_stage_user::Column::StageId.eq(stage_id.0.to_string()))
//             .filter(link_stage_user::Column::UserId.eq(u.id))
//             .one(&state.db)
//             .await
//             .map_err(|x| Left::from(x).into_response())?;

//         if link.is_none() {
//             Err(Left {
//                 status: http::StatusCode::UNAUTHORIZED,
//                 message: "Please join stage first".to_string(),
//                 uuid: "2b46d6bb",
//             }
//             .into_response())?
//         }

//         let s = stage::Entity::find_by_id(stage_id.0)
//             .one(&state.db)
//             .await
//             .map_err(|x| Left::from(x).into_response())?
//             .unwrap();

//         Ok(StageUser { user: u, stage: s })
//     }
// }

pub fn route() -> Router<AppState> {
    Router::new()
        // .route("/:id", get(crud::get_by_uuid))
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
