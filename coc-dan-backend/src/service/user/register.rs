use axum::{extract::State, Json};
use axum_extra::extract::CookieJar;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::{
    daemon::entities::User,
    left_span, mls,
    service::user::session::get_session_user,
    typedef::err::{ErrCode, Left},
    AppState,
};

#[derive(Deserialize, Serialize, Debug, ts_rs::TS)]
#[ts(
    export,
    rename = "IReqUserRegister",
    export_to = "api/user/register/IReqUserRegister.d.ts"
)]
pub struct ReqUserRegister {
    username: String,
    nickname: String,
    password: String,
}

pub async fn register(
    cookies: CookieJar,
    State(state): State<AppState>,
    Json(params): Json<ReqUserRegister>,
) -> Result<Json<User>, Left> {
    if let Some(_session) = get_session_user(&cookies, &state).await? {
        return Err(left_span!(ErrCode::InvalidParameter(
            "Please logout and try again".into()
        )));
    }

    if let Some(_user) = User::find_by_username(&state.db, params.username.clone()).await? {
        return Err(left_span!(
            ErrCode::InvalidParameter("User already exists".into()),
            "0ee1f597"
        ));
    }

    let uid = Uuid::new_v4().to_string();

    let create_statement = format!(
        r#"
        BEGIN TRANSACTION;

        CREATE type::record('user:`{uid}`') CONTENT {{
            raw_id: "{uid}",
            username: type::string($username),
            nickname: type::string($nickname),
            password: crypto::argon2::generate(type::string($password)),
            registration_time: time::now(),
            active_status: "Active",
        }};

        COMMIT TRANSACTION;
    "#
    );

    let mut query_res = state
        .db
        .manager
        .query(create_statement)
        .bind(params)
        .await
        .map_err(mls!(ErrCode::DbError))?;

    let response: Vec<User> = query_res.take(0).map_err(mls!(ErrCode::DbError))?;

    if let Some(user) = response.into_iter().next() {
        Ok(Json(user))
    } else {
        Err(left_span!(ErrCode::InternalServerError(
            "Failed to create user".into()
        )))
    }
}
