use axum::{extract::State, response::IntoResponse, Json};
use axum_extra::extract::{cookie::Cookie, CookieJar};
use rand::random;
use serde_json::json;

use crate::{
    daemon::{entities::User, DbEntity, SurrealRecord},
    left_span, mls,
    service::user::session::{get_session_user, query_session_by_raw_id},
    typedef::err::{ErrCode, Left},
    AppState,
};

#[derive(Debug, serde::Deserialize, serde::Serialize)]
pub struct ReqUserLogin {
    username: String,
    password: String,
}

pub async fn login(
    cookies: CookieJar,
    State(state): State<AppState>,
    Json(params): Json<ReqUserLogin>,
) -> Result<impl IntoResponse, Left> {
    if let Some(_user) = get_session_user(&cookies, &state).await? {
        return Err(left_span!(
            ErrCode::UnsupportedOperation("Already logged in".into()),
            "a2f80a2f"
        ));
    }

    let query = r#"
        SELECT * FROM user WHERE username = type::string($username) AND crypto::argon2::compare(password, type::string($password)) LIMIT 1;
    "#;

    let users: Vec<User> = state
        .db
        .query_manager_bind(query, params)
        .await
        .map_err(mls!(ErrCode::DbError))?;

    let user = if let Some(x) = users.first() {
        x
    } else {
        return Err(left_span!(ErrCode::InvalidParameter(
            "User does not exists or password is incorrect".into()
        )));
    };

    let session_id: usize = random();

    let session_raw_id = format!("{:x}", session_id);
    let user_id = user.db_thing();
    let session_id = format!("session:{}", session_raw_id);

    let query = r#"
        CREATE type::record($id) CONTENT {
            raw_id: type::string($raw_id),
            login_time: time::now(),
            expiration_time: time::now() + 30d,
            session_type: {
                User: type::record($user_id)
            }
        };
        "#;

    let sessions: Vec<SurrealRecord> = state
        .db
        .query_manager_bind(
            &query,
            json!({
                "id": session_id,
                "raw_id": session_raw_id,
                "user_id": user_id.to_string(),
            }),
        )
        .await?;

    if let Some(_) = sessions.first() {
    } else {
        return Err(left_span!(ErrCode::InternalServerError(
            "Failed to create session".into()
        )));
    };

    let session = query_session_by_raw_id(session_raw_id, &state).await?;

    if let Some(v) = session {
        let mut new_session = Cookie::new("SESSION", v.raw_id.clone());
        new_session.set_path("/");
        new_session.set_expires(None);
        new_session.set_max_age(None);
        Ok((cookies.add(new_session), Json(v)))
    } else {
        Err(left_span!(ErrCode::InternalServerError(
            "Failed to create session".into()
        )))
    }
}
