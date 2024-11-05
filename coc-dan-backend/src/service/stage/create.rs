use axum::{extract::State, Json};
use chrono::Utc;
use serde::{Deserialize, Serialize};
use serde_json::json;

use crate::{
    daemon::{
        entities::{Stage, StageRule, User},
        relations::RelUserToStage,
        DbEntity, DbRelation, SurrealRecord,
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
            rule: type::string($rule),
            owner: type::record($owner),
        }} {version};

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
            "rule": req.rule,
        }))
        .await
        .map_err(mls!(ErrCode::DbError))?;

    let response: Vec<SurrealRecord> = query_res.take(3).map_err(mls!(ErrCode::DbError))?;

    if let Some(record) = response.into_iter().next() {
        let stage =
            Stage::db_load_by_id(record.id.id.to_raw().parse().unwrap(), &state.db.manager).await?;
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
