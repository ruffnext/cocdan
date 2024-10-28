use axum::{
    async_trait,
    extract::{FromRef, FromRequestParts},
    response::{IntoResponse, Response},
};
use axum_extra::extract::CookieJar;
use http::{request::Parts, StatusCode};
use serde_json::json;

use crate::{
    daemon::{
        entities::{Session, SessionType, UserActiveStatus},
        DbEntity,
    },
    left_span, mls,
    typedef::err::{ErrCode, Left},
    AppState,
};

#[async_trait]
impl<S> FromRequestParts<S> for Session
where
    S: Send + Sync,
    AppState: FromRef<S>,
{
    type Rejection = Response;

    async fn from_request_parts(req: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        let cookies = CookieJar::from_request_parts(req, state).await.unwrap();
        let state = AppState::from_ref(state);
        let session = get_session_user(&cookies, &state)
            .await
            .map_err(|x| x.into_response())?;
        if let Some(v) = session {
            Ok(v)
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

pub async fn query_session_by_raw_id(
    raw_id: String,
    state: &AppState,
) -> Result<Option<Session>, Left> {
    let query_str = "
        SELECT * FROM session WHERE raw_id == $id LIMIT 1 FETCH session_type.User;
    ";

    let session: Vec<Session> = state
        .db
        .query_manager_bind(query_str, json!({ "id": raw_id }))
        .await
        .map_err(mls!(ErrCode::DbError))?;

    if let Some(v) = session.into_iter().next() {
        match &v.session_type {
            SessionType::User(u) => match u.active_status {
                UserActiveStatus::Active => {
                    if v.is_expired() {
                        Session::db_del(v.db_id(), &state.db.manager).await?;
                        return Err(left_span!(
                            ErrCode::PermissionDenied("Session is expired".into()),
                            "224c7d40"
                        ));
                    }
                }
                UserActiveStatus::Banned => {
                    Session::db_del(v.db_id(), &state.db.manager).await?;
                    return Err(left_span!(
                        ErrCode::PermissionDenied("User is banned".into()),
                        "6d0ce117"
                    ));
                }
            },
        }
        Ok(Some(v))
    } else {
        Ok(None)
    }
}

pub async fn get_session_user(
    cookies: &CookieJar,
    state: &AppState,
) -> Result<Option<Session>, Left> {
    let session_str = if let Some(x) = cookies
        .get("SESSION")
        .and_then(|x| Some(x.value().to_string()))
    {
        x
    } else {
        return Ok(None);
    };

    query_session_by_raw_id(session_str, state).await
}
