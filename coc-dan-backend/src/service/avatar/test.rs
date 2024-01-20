use coc_dan_common::def::{stage::IStage, avatar::{IAvatar, service::ICreateAvatar}};
use serde_json::json;
use http::StatusCode;
use tracing_test::traced_test;

use crate::service::{tests::new_test_server, user::tests::test_create_user_and_login};

#[tokio::test]
#[traced_test]
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
    let stage_0 : IStage = res.json();

    // create avatar 0
    let res = server.post("/api/avatar/new").json(&ICreateAvatar {
        name : "avatar 0".to_string(),
        detail : None,
        stage_id : Some(stage_0.id)
    }).add_cookie(session.clone()).await;
    assert_eq!(res.status_code(), StatusCode::OK);
    let avatar_0 : IAvatar = res.json();
    assert_eq!(avatar_0.owner, u.id);

    let res = server.post("/api/avatar/new").json(&ICreateAvatar {
        name : "avatar 1".to_string(),
        detail : None,
        stage_id : Some(stage_0.id)
    }).add_cookie(session.clone()).await;
    let avatar_1 : IAvatar = res.json();
    
    // list owned avatars
    let res = server.get("/api/avatar/list_owned").add_cookie(session.clone()).await;
    assert_eq!(res.status_code(), StatusCode::OK);
    let avatars : Vec<IAvatar> = res.json();
    assert_eq!(avatars.len(), 2);
    assert!(avatars[0] == avatar_0);
    assert!(avatars[1] == avatar_1);
    

    // delete an avatar
    let res = server.delete(format!("/api/avatar/{}", avatar_0.id).as_str()).add_cookie(session.clone()).await;
    assert_eq!(res.status_code(), StatusCode::OK);
    
    let res = server.get("/api/avatar/list_owned").add_cookie(session.clone()).await;
    assert_eq!(res.status_code(), StatusCode::OK);
    let avatars_1 : Vec<IAvatar> = res.json();
    assert_eq!(avatars_1.len(), 1);
    assert!(avatars_1[0] == avatar_1);

    // leave stage then all avatar will be removed
    let res = server.post(format!("/api/stage/{}/leave", stage_0.id).as_str()).add_cookie(session.clone()).await;
    assert_eq!(res.status_code(), StatusCode::OK);

    let res = server.get("/api/avatar/list_owned").add_cookie(session.clone()).await;
    assert_eq!(res.status_code(), StatusCode::NO_CONTENT);
}
