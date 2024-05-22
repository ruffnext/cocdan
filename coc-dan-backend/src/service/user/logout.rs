use axum::{extract::State, response::IntoResponse};
use axum_extra::extract::{cookie::Cookie, CookieJar};
use sea_orm::EntityTrait;

use crate::{
    database::entities::{prelude::*, *},
    AppState,
};

pub async fn logout(
    cookie: CookieJar,
    _u: user::Model,
    State(state): State<AppState>,
) -> impl IntoResponse {
    match cookie.get("SESSION") {
        Some(v) => {
            let _r = Session::delete_by_id(v.value()).exec(&state.db).await;
        }
        None => {}
    }
    cookie.remove(Cookie::from("SESSION")).into_response()
}
