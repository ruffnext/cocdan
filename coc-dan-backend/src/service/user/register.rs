
use axum::{extract::{self, State}, Json};
use axum_extra::extract::CookieJar;
use sea_orm::{EntityTrait, QueryFilter, ColumnTrait, ActiveValue, ActiveModelTrait};

use crate::{err::Left, entities::{prelude::*, *}, AppState};

use super::{is_login, IUser};

#[derive(serde::Deserialize, Debug)]
pub struct UserRegister {
    name : String
}

pub async fn register (
    cookies : CookieJar, 
    State(state) : State<AppState>,
    extract::Json(params) : extract::Json<UserRegister>
) -> Result<Json<IUser>, Left> {
    if is_login(&cookies, &state.db).await == true {
        return Err(Left { 
            status: http::StatusCode::BAD_REQUEST, 
            message: "Please logout and try again".to_string(), 
            uuid: "edc3b963" 
        })
    }
    let db = &state.db;
    match User::find().filter(user::Column::Name.eq(params.name.as_str())).one(db).await? {
        Some(_v) => {
            return Err(Left { 
                status: http::StatusCode::BAD_REQUEST, 
                message: format!("User {} has been registered, please try another", params.name), 
                uuid: "0ee1f597" 
            })
        },
        None => {
            let u = user::ActiveModel {
                name : ActiveValue::Set(params.name.clone()),
                nick_name : ActiveValue::Set(params.name.clone()),
                ..Default::default()
            };
            let res = u.insert(db).await?;
            Ok(Json(res.into()))
        }
    }
}
