mod login;
mod logout;
mod me;
mod register;
mod session;

use axum::{
    routing::{get, post},
    Router,
};

use crate::AppState;

pub fn route() -> Router<AppState> {
    Router::new()
        .route("/me", get(me::get_me))
        .route("/register", post(register::register))
        .route("/login", post(login::login))
        .route("/logout", post(logout::logout))
}

#[cfg(test)]
#[cfg(feature = "mock")]
pub(crate) mod tests {
    use axum_extra::extract::CookieJar;
    use axum_test::TestServer;
    use http::StatusCode;
    use serde_json::json;

    use crate::{
        daemon::entities::User,
        service::tests::{new_test_server, test_extract_left_code},
    };

    pub async fn test_create_user_and_login(
        username: &str,
        server: &TestServer,
    ) -> (User, CookieJar) {
        let password = "password";
        let u: User = server
            .post("/api/user/register")
            .json(&json!({
                "username" : username.to_string(),
                "password" : password.to_string(),
                "nickname" : "nickname".to_string()
            }))
            .await
            .json();
        let response = server
            .post("/api/user/login")
            .json(&json!({
                "username" : u.username,
                "password" : password
            }))
            .await;
        let cookie = CookieJar::new().add(response.cookie("SESSION"));
        (u, cookie)
    }

    #[tokio::test]
    async fn test_user_basic() {
        let (server, _db) = new_test_server().await;
        let new_user_name = "user name".to_string();
        let new_user_pass = "user pass".to_string();
        let new_user_nick = "user nick".to_string();

        // create user
        let response = server
            .post("/api/user/register")
            .json(&json!({
                "username" : new_user_name,
                "password" : new_user_pass,
                "nickname" : new_user_nick
            }))
            .await;
        assert!(response.status_code() == StatusCode::OK);
        let u: User = response.json();
        assert!(u.username == new_user_name);

        // login
        let response = server
            .post("/api/user/login")
            .json(&json!({
                "username" : u.username,
                "password" : new_user_pass
            }))
            .await;
        assert!(response.status_code() == StatusCode::OK);
        let session = response.cookie("SESSION"); // if no cookie found, it will panic

        // login twice wll cause bad request
        let response = server
            .post("/api/user/login")
            .json(&json!({
                "username" : u.username,
                "password" : new_user_pass,
                "nickname" : new_user_nick
            }))
            .add_cookie(session.clone())
            .await;
        assert_eq!(test_extract_left_code(&response), "a2f80a2f");

        // logout
        let response = server
            .post("/api/user/logout")
            .add_cookie(session.clone())
            .await;
        assert!(response.status_code() == StatusCode::OK);

        // logout twice
        let response = server.post("/api/user/logout").add_cookie(session).await;
        assert!(response.status_code() == StatusCode::UNAUTHORIZED);
        assert_eq!(test_extract_left_code(&response), "c6c3cb95");

        // if the user name has been used, reply 400 BAD REQUEST
        let response = server
            .post("/api/user/register")
            .json(&json!({
                "username" : new_user_name,
                "password" : new_user_pass,
                "nickname" : new_user_nick
            }))
            .await;
        assert!(response.status_code() == StatusCode::BAD_REQUEST);
        assert_eq!(test_extract_left_code(&response), "0ee1f597");
    }
}
