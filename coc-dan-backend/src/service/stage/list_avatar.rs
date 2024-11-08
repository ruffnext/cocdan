use axum::{
    extract::{Path, State},
    Json,
};
use serde_json::json;

use crate::{
    daemon::{
        entities::{Avatar, Session, SessionType, Stage},
        DbEntity,
    },
    left_span,
    typedef::err::{ErrCode, Left},
    AppState,
};

pub async fn list_my_stage_avatars(
    State(state): State<AppState>,
    Path(stage_id): Path<String>,
    session: Session,
) -> Result<Json<Vec<Avatar>>, Left> {
    let user = match session.session_type {
        SessionType::User(u) => u,
    };

    let stage = if let Some(stage) = Stage::db_load_by_id(stage_id, &state.db.manager).await? {
        stage
    } else {
        return Err(left_span!(ErrCode::InvalidParameter(
            "Stage not found".into()
        )));
    };

    let query = "SELECT * FROM avatar WHERE owner = type::record($owner) AND stage = type::record($stage) FETCH owner, stage, stage.owner";

    let avatars: Vec<Avatar> = state
        .db
        .query_manager_bind(
            &query,
            json!({
                "owner": user.db_thing().to_string(),
                "stage": stage.db_thing().to_string(),
            }),
        )
        .await?;

    return Ok(Json(avatars));
}
