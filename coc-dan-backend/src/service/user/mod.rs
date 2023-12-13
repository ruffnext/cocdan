mod login;
mod register;
mod logout;

use axum::{Router, routing::{post, get}, extract::{FromRequestParts, State, FromRef}, async_trait, response::{IntoResponse, Response}, Json};
use axum_extra::extract::CookieJar;
use http::request::Parts;
use sea_orm::{EntityTrait, DatabaseConnection};
use ts_rs::TS;
use crate::{entities::{prelude::*, user}, err::Left, AppState};

#[async_trait]
impl<S> FromRequestParts<S> for crate::entities::user::Model 
where S: Send + Sync,
    AppState: FromRef<S>
{
    type Rejection = Response;

    async fn from_request_parts(req: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        let cookies = CookieJar::from_request_parts(req, state).await.unwrap();
        let state = AppState::from_ref(state);
        get_session_user(&cookies, State(state)).await.map_err(|x| x.into_response())
    }
}


#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq)]
#[ts(export)]
pub struct IUser {
    pub id : i32,
    pub name : String,
    pub nick_name : String
}

impl From<user::Model> for IUser {
    fn from(value: user::Model) -> Self {
        Self {
            id : value.id,
            name : value.name.clone(),
            nick_name : value.nick_name.clone()
        }
    }
}

pub async fn get_session_user (
    cookies : &CookieJar,
    State(state) : State<AppState>
) -> Result<user::Model, Left> {
    let session_str : Option<String> = cookies.get("SESSION").and_then(|c| Some(c.value().to_string()));
    match session_str {
        Some(v) => {
            let db = &state.db;
            match Session::find_by_id(v).one(db).await? {
                Some(v) => {
                    match User::find_by_id(v.user_id).one(db).await? {
                        Some(u) => {
                            return Ok(u)
                        },
                        None => {}
                    }
                },
                None => {}
            }
        },
        None => {}
    };
    Err(Left {
        status : http::StatusCode::UNAUTHORIZED,
        message : "Please login first".to_string(),
        uuid : "c6c3cb95"
    })
}

pub async fn is_login(cookies : &CookieJar, db : &DatabaseConnection) -> bool {
    let session_str = match cookies.get("SESSION").and_then(|c| Some(c.value().to_string())) {
        Some(v) => v,
        None => return false
    };
    match Session::find_by_id(session_str).one(db).await {
        Ok(res) => {
            res.is_some()
        },
        Err(_e) => false
    }
}

async fn get_me(u : user::Model) -> Json<IUser> {
    Json(u.into())
}

pub fn route() -> Router<AppState> {
    Router::new()
        .route("/me", get(get_me))
        .route("/register", post(register::register))
        .route("/login", post(login::login))
        .route("/logout", post(logout::logout))
}

#[cfg(test)]
pub (crate) mod tests {
    use axum_extra::extract::CookieJar;
    use axum_test::TestServer;
    use serde_json::json;
    use http::StatusCode;

    use crate::service::tests::{new_test_server, test_extract_left_uuid};

    use super::IUser;
    
    pub async fn test_create_user_and_login(user_name : &str, server : &TestServer) -> (IUser, CookieJar) {
        let u : IUser = server.post("/api/user/register").json(&json!({
            "name" : user_name.to_string()
        })).await.json();
        let response = server.post("/api/user/login").json(&json!({
            "name" : u.name
        })).await;
        let cookie = CookieJar::new().add(response.cookie("SESSION"));
        (u, cookie)
    }

    #[tokio::test]
    async fn test_user_basic() {
        let (server, _db) = new_test_server().await;
        let new_user_name = "user name".to_string();
        
        // create user
        let response = server.post("/api/user/register").json(&json!({
            "name" : new_user_name
        })).await;
        assert!(response.status_code() == StatusCode::OK);
        let u : IUser = response.json();
        assert!(u.name == new_user_name);

        // login
        let response = server.post("/api/user/login").json(&json!({
            "name" : u.name
        })).await;
        assert!(response.status_code() == StatusCode::OK);
        let session = response.cookie("SESSION");   // if no cookie found, it will panic

        // login twice wll cause bad request
        let response = server.post("/api/user/login").json(&json!({
            "name" : u.name
        })).add_cookie(session.clone()).await;
        assert_eq!(test_extract_left_uuid(&response), "a2f80a2f");

        // logout
        let response = server.post("/api/user/logout").add_cookie(session.clone()).await;
        assert!(response.status_code() == StatusCode::OK);
        
        // logout twice
        let response = server.post("/api/user/logout").add_cookie(session).await;
        assert!(response.status_code() == StatusCode::UNAUTHORIZED);
        assert_eq!(test_extract_left_uuid(&response), "c6c3cb95");

        // if the user name has been used, reply 400 BAD REQUEST
        let response = server.post("/api/user/register").json(&json!({
            "name" : new_user_name
        })).await;
        assert!(response.status_code() == StatusCode::BAD_REQUEST);
        assert_eq!(test_extract_left_uuid(&response), "0ee1f597");
    }
}