use axum::{
    extract::{FromRef, FromRequestParts},
    response::{IntoResponse, Response},
    Json,
};
use axum_extra::extract::CookieJar;
use http::{request::Parts, StatusCode};

use crate::{
    daemon::entities::{Session, SessionType, User},
    typedef::err::Left,
    AppState,
};

use super::session::get_session_user;

pub async fn get_me(session: Session) -> Json<Session> {
    Json(session)
}

impl<S> FromRequestParts<S> for User
where
    S: Send + Sync,
    AppState: FromRef<S>,
{
    type Rejection = Response;

    async fn from_request_parts(req: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        let cookies = CookieJar::from_request_parts(req, state).await.unwrap();
        let state = AppState::from_ref(state);
        let session: Option<Session> = get_session_user(&cookies, &state)
            .await
            .map_err(|x| x.into_response())?;
        if let Some(v) = session {
            match v.session_type {
                SessionType::User(u) => Ok(u),
            }
        } else {
            Err((Left {
                status: StatusCode::UNAUTHORIZED,
                message: "Unauthorized".to_string(),
                code: Some("c6c3cb95".to_string()),
            })
            .into_response())
        }
    }
}
