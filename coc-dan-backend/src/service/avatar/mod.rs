use axum::{Router, routing::{get, post}, response::IntoResponse, Json};

mod crud;

pub use crud::clear_user_stage_avatars;

use crate::AppState;

impl IntoResponse for crate::entities::avatar::Model {
    fn into_response(self) -> axum::response::Response {
        (http::StatusCode::OK, Json(self)).into_response()
    }
}

pub fn route() -> Router<AppState> {
    Router::new()
        .route("/:id",          get     (crud::get_by_id).
                                                    delete  (crud::destroy))
        .route("/list_owned",   get     (crud::list_by_user))
        .route("/new",          post    (crud::create))
}

#[cfg(test)]
pub (crate) mod tests {
    use std::str::FromStr;

    use serde_json::json;
    use uuid::Uuid;
    use http::StatusCode;

    use crate::{service::{tests::new_test_server, user::tests::test_create_user_and_login}, entities::{stage, avatar}};

    use super::crud::CreateAvatar;

    #[tokio::test]
    async fn test_avatar_basic() {
        let (server, _db) = new_test_server().await;
        let (u, cookie) = test_create_user_and_login("user_name", &server).await;
        let session = cookie.get("SESSION").unwrap();

        let res = server.post("/api/stage/new")
            .json(&json!({
                "title" : "stage title",
                "description" : "stage description"
            }))
            .add_cookie(session.clone()).await;
        assert_eq!(res.status_code(), StatusCode::OK);
        let stage_0 : stage::Model = res.json();

        // create avatar 0
        let res = server.post("/api/avatar/new").json(&CreateAvatar {
            name : "avatar 0".to_string(),
            description : "avatar description".to_string(),
            stage_id : Uuid::from_str(&stage_0.uuid).unwrap()
        }).add_cookie(session.clone()).await;
        assert_eq!(res.status_code(), StatusCode::OK);
        let avatar_0 : avatar::Model = res.json();
        assert_eq!(avatar_0.owner, u.id);

        let res = server.post("/api/avatar/new").json(&CreateAvatar {
            name : "avatar 1".to_string(),
            description : "avatar description".to_string(),
            stage_id : Uuid::from_str(&stage_0.uuid).unwrap()
        }).add_cookie(session.clone()).await;
        let avatar_1 : avatar::Model = res.json();
        
        // list owned avatars
        let res = server.get("/api/avatar/list_owned").add_cookie(session.clone()).await;
        assert_eq!(res.status_code(), StatusCode::OK);
        let avatars : Vec<avatar::Model> = res.json();
        assert_eq!(avatars.len(), 2);
        assert!(avatars[0] == avatar_0);
        assert!(avatars[1] == avatar_1);
        

        // delete an avatar
        let res = server.delete(format!("/api/avatar/{}", avatar_0.id).as_str()).add_cookie(session.clone()).await;
        assert_eq!(res.status_code(), StatusCode::OK);
        
        let res = server.get("/api/avatar/list_owned").add_cookie(session.clone()).await;
        assert_eq!(res.status_code(), StatusCode::OK);
        let avatars_1 : Vec<avatar::Model> = res.json();
        assert_eq!(avatars_1.len(), 1);
        assert!(avatars_1[0] == avatar_1);

        // leave stage then all avatar will be removed
        let res = server.post(format!("/api/stage/{}/leave", stage_0.uuid).as_str()).add_cookie(session.clone()).await;
        assert_eq!(res.status_code(), StatusCode::OK);

        let res = server.get("/api/avatar/list_owned").add_cookie(session.clone()).await;
        assert_eq!(res.status_code(), StatusCode::NO_CONTENT);
    }
}
