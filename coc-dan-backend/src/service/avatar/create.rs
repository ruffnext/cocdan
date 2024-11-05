use axum::{extract::State, Json};
use serde_json::json;
use surrealdb::sql::Thing;

use crate::{
    daemon::{
        entities::{Avatar, AvatarDetail, Session, SessionType, Stage},
        DbEntity, SurrealRecord,
    },
    left_span, mls,
    typedef::err::{ErrCode, Left},
    AppState,
};

#[derive(serde::Deserialize, ts_rs::TS)]
#[ts(
    export,
    rename = "IReqCreateAvatar",
    export_to = "api/avatar/create/IReqCreateAvatar.d.ts"
)]
pub struct ReqCreateAvatar {
    stage_id: i64,
    name: String,
    #[serde(default)]
    detail: AvatarDetail,
    #[serde(default)]
    header: String,
}

pub async fn create_avatar(
    State(state): State<AppState>,
    session: Session,
    Json(req): Json<ReqCreateAvatar>,
) -> Result<Json<Avatar>, Left> {
    let stage = if let Some(v) = Stage::db_load_by_id(req.stage_id, &state.db.manager).await? {
        v
    } else {
        return Err(left_span!(ErrCode::InvalidParameter("stage_id".into())));
    };

    if !session.is_on_stage(stage.raw_id, &state.db.manager).await {
        return Err(left_span!(ErrCode::PermissionDenied(
            "You have no access to create avatar on this stage".into()
        )));
    }

    let owner = match session.session_type {
        SessionType::User(ref u) => u,
    };

    let new_avatar_id = uuid::Uuid::new_v4().to_string();

    let save_query = format!(
        "
        CREATE type::record($id) CONTENT {{
            raw_id: $raw_id,
            name: $name,
            detail: $detail,
            stage: type::record($stage),
            owner: type::record($owner),
            header: $header,
        }} VERSION d'{time}';
    ",
        time = chrono::Utc::now().to_rfc3339_opts(chrono::SecondsFormat::Secs, true)
    );

    let mut response = state
        .db
        .manager
        .query(save_query)
        .bind(json!({
            "id": Thing::from((Avatar::db_tab_name(), new_avatar_id.as_ref())).to_string(),
            "raw_id": new_avatar_id,
            "name": req.name,
            "detail": req.detail,
            "stage": stage.db_thing().to_string(),
            "owner": owner.db_thing().to_string(),
            "header": req.header,
        }))
        .await
        .map_err(mls!(ErrCode::DbError))?;

    let new_avatar: Vec<SurrealRecord> = response.take(0).map_err(mls!(ErrCode::DbError))?;

    if let Some(v) = new_avatar.into_iter().next() {
        if let Some(v) =
            Avatar::db_load_by_id(v.id.id.to_raw().parse().unwrap(), &state.db.manager).await?
        {
            return Ok(Json(v));
        } else {
            return Err(left_span!(ErrCode::InternalServerError(
                "Failed to load new avatar".into()
            )));
        }
    } else {
        return Err(left_span!(ErrCode::InternalServerError(
            "Failed to create new avatar".into()
        )));
    }
}
