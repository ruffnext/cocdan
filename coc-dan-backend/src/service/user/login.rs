use axum::{extract::{self, State}, Json, response::IntoResponse};
use axum_extra::extract::{CookieJar, cookie::Cookie};
use sea_orm::{EntityTrait, QueryFilter, ColumnTrait, ActiveValue, ActiveModelTrait};

use crate::{err::Left, entities::{prelude::*, *}, AppState};

use super::is_login;

#[derive(serde::Deserialize, Debug)]
pub struct UserLogin {
    name : String
}

pub async fn login (
    cookies : CookieJar, 
    State(state) : State<AppState>,
    extract::Json(params) : extract::Json<UserLogin>
) -> Result<impl IntoResponse, Left> {
    if is_login(&cookies, &state.db).await == true {
        return Err(Left { 
            status: http::StatusCode::BAD_REQUEST, 
            message: "Please logout and try again".to_string(), 
            uuid: "a2f80a2f" 
        })
    }
    let db = &state.db;
    match User::find().filter(user::Column::Name.eq(params.name.as_str())).one(db).await? {
        Some(v) => {
            let s = session::ActiveModel {
                uuid : ActiveValue::Set(uuid::Uuid::new_v4().to_string()),
                user_id : ActiveValue::Set(v.id),
            };
            let s = s.insert(db).await?;
            Ok((cookies.add(Cookie::new("SESSION", s.uuid.clone())), Json(v)))
        },
        None => {
            return Err(Left { 
                status: http::StatusCode::BAD_REQUEST, 
                message: format!("User {} does not exists", params.name), 
                uuid: "7755bd0a"
            })
        }
    }
}
