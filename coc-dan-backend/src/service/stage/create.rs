use axum::{extract::State, Json};
use chrono::Utc;
use serde::{Deserialize, Serialize};
use serde_json::json;
use uuid::Uuid;

use crate::{
    daemon::{
        entities::{Stage, StageRule, User},
        relations::RelUserToStage,
        DbEntity, DbRelation, SurrealRecord,
    },
    left_span,
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
    pub rule: StageRule,
}

pub async fn create_stage(
    State(state): State<AppState>,
    user: User,
    Json(req): Json<ReqCreateStage>,
) -> Result<Json<Stage>, Left> {
    #[cfg(feature = "mock")]
    let version = "".to_string();
    #[cfg(not(feature = "mock"))]
    let version = format!(
        " VERSION d'{}'",
        Utc::now().to_rfc3339_opts(chrono::SecondsFormat::Secs, true)
    );

    let raw_id = Uuid::new_v4().to_string();

    let create_statement = format!(
        r#"
        CREATE type::record("stage:`{raw_id}`") CONTENT {{
            raw_id: "{raw_id}",
            title: type::string($title),
            description: type::string($description),
            rule: type::string($rule),
            owner: type::record($owner),
        }} {version};
    "#
    );

    let response: Vec<SurrealRecord> = state
        .db
        .query_manager_bind(
            &create_statement,
            json!({
                "title": req.title,
                "description": req.description,
                "owner": user.db_thing().to_string(),
                "rule": req.rule,
            }),
        )
        .await?;

    if let Some(record) = response.into_iter().next() {
        let stage = Stage::db_load_by_id(record.id.id.to_raw(), &state.db.manager).await?;
        if let Some(stage) = stage {
            RelUserToStage::rel_save(&user, &stage, &(), &state.db.manager).await?;
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
