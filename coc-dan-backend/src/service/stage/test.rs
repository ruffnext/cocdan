use crate::service::{user::tests::test_create_user_and_login, tests::{new_test_server, test_extract_left_uuid}, stage::IStage};
use coc_dan_common::def::{user::IUser, stage::service::ICreateStage};
use http::StatusCode;
use serde_json::json;

#[tokio::test]
async fn test_stage_basic() {
    let (server, _db) = new_test_server().await;
    let (u, cookie) = test_create_user_and_login("user name", &server).await;
    let session = cookie.get("SESSION").unwrap().clone();
    
    let title = "stage title".to_string();
    let description = "stage description".to_string();
    
    // create a stage
    let response = server.post("/api/stage/new")
        .json(&ICreateStage {
            title : title.clone(), 
            description : description.clone()
        }
    ).add_cookie(session.clone()).await;
    assert!(response.status_code() == StatusCode::OK);
    
    let new_stage : IStage = response.json();
    assert!(new_stage.title == title);
    assert!(new_stage.description == description);
    
    // list owned stage
    let response = server.get("/api/stage/my_stages").add_cookie(session.clone()).await;
    assert!(response.status_code() == StatusCode::OK);
    let stages : Vec<IStage> = response.json();
    assert!(stages.len() == 1);
    let first_stage = stages.first().unwrap();
    assert!(first_stage.title == title);
    assert!(first_stage.description == description);
    assert!(first_stage.owner == u.id);
    
    // get stage by id
    let response = server.get(format!("/api/stage/{}", new_stage.id).as_str()).add_cookie(session.clone()).await;
    let get_by_id_stage : IStage = response.json();
    assert!(get_by_id_stage == *first_stage);

    // get stage users
    let res = server.get(format!("/api/stage/{}/users", new_stage.id).as_str()).add_cookie(session.clone()).await;
    let stage_users : Vec<IUser> = res.json();
    assert!(stage_users.len() == 1);
    assert!(stage_users[0] == u);

    // user 2 join
    let (u2, cookie2) = test_create_user_and_login("user name 2", &server).await;
    let session2 = cookie2.get("SESSION").unwrap();
    let res = server.post(format!("/api/stage/{}/join", first_stage.id).as_str()).add_cookie(session2.clone()).await;
    assert!(res.status_code() == StatusCode::OK);
    
    // user 3 join
    let (u3, cookie3) = test_create_user_and_login("user name 3", &server).await;
    let session3 = cookie3.get("SESSION").unwrap();
    let res = server.post(format!("/api/stage/{}/join", first_stage.id).as_str()).add_cookie(session3.clone()).await;
    assert!(res.status_code() == StatusCode::OK);
    
    // user 3 create a new stage, and deleting it using user 1, resulting bad request
    {
        let res = server.post("/api/stage/new").json(&json!({
            "title" : "stage title".to_string(),
            "description" : "".to_string()
        })).add_cookie(session3.clone()).await;
        let user_3_stage : IStage = res.json();
        let res = server.get("/api/stage/my_stages").add_cookie(session3.clone()).await;
        let user_3_stages : Vec<IStage> = res.json();
        assert_eq!(user_3_stages.len(), 2);
        assert!(user_3_stages[0] == new_stage);
        assert!(user_3_stages[1] == user_3_stage);
        
        let res = server.post(format!("/api/stage/{}/leave", user_3_stage.id).as_str()).add_cookie(session.clone()).await;
        assert_eq!(res.status_code(), StatusCode::BAD_REQUEST);
        assert_eq!(test_extract_left_uuid(&res), "21fa1f82");

        let res = server.post(format!("/api/stage/{}/leave", user_3_stage.id).as_str()).add_cookie(session3.clone()).await;
        assert_eq!(res.status_code(), StatusCode::OK);
        
        let res = server.get("/api/stage/my_stages").add_cookie(session3.clone()).await;
        let user_3_stages : Vec<IStage> = res.json();
        assert_eq!(user_3_stages.len(), 1);
        assert!(user_3_stages[0] == new_stage);
    }
    

    // get stage users
    let res = server.get(format!("/api/stage/{}/users", new_stage.id).as_str()).add_cookie(session.clone()).await;
    let stage_users : Vec<IUser> = res.json();
    assert!(stage_users.len() == 3);
    assert!(stage_users[0] == u);
    assert!(stage_users[1] == u2);
    assert!(stage_users[2] == u3);
    
    // get stages by user
    let response = server.get("/api/stage/my_stages").add_cookie(session3.clone()).await;
    assert!(response.status_code() == StatusCode::OK);
    let stages : Vec<IStage> = response.json();
    assert!(stages[0] == new_stage);
    
    // user 3 leaves
    let res = server.post(format!("/api/stage/{}/leave", new_stage.id).as_str()).add_cookie(session3.clone()).await;
    assert!(res.status_code() == StatusCode::OK);
    assert!(server.get("/api/stage/my_stages").add_cookie(session3.clone()).await.status_code() == StatusCode::NO_CONTENT);

    // check remaining users
    let res = server.get("/api/stage/my_stages").add_cookie(session3.clone()).await;
    assert!(res.status_code() == StatusCode::NO_CONTENT);

    let res = server.get(format!("/api/stage/{}/users", new_stage.id).as_str()).add_cookie(session.clone()).await;
    let users : Vec<IUser> = res.json();
    assert!(users.len() == 2);
    assert!(users[0] == u);
    assert!(users[1] == u2);

    let res = server.get("/api/stage/my_stages").add_cookie(session2.clone()).await;
    assert!(res.status_code() == StatusCode::OK);
    let stages : Vec<IStage> = res.json();
    assert!(stages.len() == 1);
    assert!(stages[0] == new_stage);

    // When the owner leaves stage, the stage will be destroyed
    let res = server.post(format!("/api/stage/{}/leave", new_stage.id).as_str()).add_cookie(session.clone()).await;
    assert!(res.status_code() == StatusCode::OK);

    let res = server.get(format!("/api/stage/{}", new_stage.id).as_str()).add_cookie(session.clone()).await;
    assert!(res.status_code() == StatusCode::NO_CONTENT);

    let res = server.get("/api/stage/my_stages").add_cookie(session2.clone()).await;
    assert!(res.status_code() == StatusCode::NO_CONTENT);
}
