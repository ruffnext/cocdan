use axum::{extract::State, Json};
use serde::{Deserialize, Serialize};
use serde_json::json;

use crate::{
    daemon::{
        entities::{Stage, User},
        DbEntity, SurrealRecord,
    },
    left_span, mls,
    typedef::err::{ErrCode, Left},
    AppState,
};

#[derive(Deserialize, Serialize, ts_rs::TS)]
#[ts(
    export,
    rename = "IReqCreateStage",
    export_to = "api/stage/create/IReqCreateStage.d.ts"
)]
pub struct ReqCreateStage {
    pub title: String,
    pub description: String,
}

pub async fn create_stage(
    State(state): State<AppState>,
    user: User,
    Json(req): Json<ReqCreateStage>,
) -> Result<Json<Stage>, Left> {
    let create_statement = format!(
        r#"
        BEGIN TRANSACTION;

        let $max_id = math::max(SELECT VALUE raw_id as max_id FROM stage WHERE raw_id);

        let $max_id = return if $max_id == None {{
            1
        }} else {{
            type::int($max_id) + 1
        }};

        let $id = type::record(string::concat("stage:", type::string($max_id)));

        CREATE $id CONTENT {{
            raw_id: $max_id,
            title: type::string($title),
            description: type::string($description),
            owner: type::record($owner),
        }};

        COMMIT TRANSACTION;
    "#
    );

    let mut query_res = state
        .db
        .manager
        .query(create_statement)
        .bind(json!({
            "title": req.title,
            "description": req.description,
            "owner": user.db_thing().to_string(),
        }))
        .await
        .map_err(mls!(ErrCode::DbError))?;

    let response: Vec<SurrealRecord> = query_res.take(3).map_err(mls!(ErrCode::DbError))?;

    if let Some(record) = response.into_iter().next() {
        let stage = Stage::db_load_by_id(record.id.id, &state.db.manager).await?;
        if let Some(stage) = stage {
            return Ok(Json(stage));
        } else {
            return Err(left_span!(ErrCode::InternalServerError(
                "Stage not found".into()
            )));
        }
    } else {
        Err(left_span!(ErrCode::DbError))
    }
}
