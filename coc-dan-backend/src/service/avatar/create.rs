use axum::{extract::State, Json};
use serde_json::json;
use surrealdb::sql::Thing;

use crate::{
    daemon::{
        entities::{Avatar, AvatarDetail, Session, SessionType, Stage, TxAction, TxAux},
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
    stage_id: String,
    #[serde(default)]
    detail: AvatarDetail,
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

    if !session
        .is_on_stage(stage.raw_id.clone(), &state.db.manager)
        .await
    {
        return Err(left_span!(ErrCode::PermissionDenied(
            "You have no access to create avatar on this stage".into()
        )));
    }

    let owner = match session.session_type {
        SessionType::User(ref u) => u,
    };

    let new_avatar_id = uuid::Uuid::new_v4().to_string();

    let mut tx = TxAux::new(
        stage.raw_id.clone(),
        owner.raw_id.clone(),
        new_avatar_id.clone(),
        TxAction::AvatarAdd((new_avatar_id.clone(), req.detail.clone())),
        &state.db.manager,
    )
    .await?;

    let save_query = format!(
        "
        BEGIN TRANSACTION;

        let $res = INSERT INTO avatar_version $detail;
        
        CREATE type::record($id) SET
            raw_id = $raw_id,
            versions = [{{
                time: time::now(),
                version: type::record(array::first($res.id)),
                tx: type::record($tx),
            }}],
            stage = type::record($stage),
            owner = type::record($owner);
            
        COMMIT TRANSACTION;",
    );

    let mut response = state
        .db
        .manager
        .query(save_query)
        .bind(json!({
            "id": Thing::from((Avatar::db_tab_name(), new_avatar_id.as_ref())).to_string(),
            "raw_id": new_avatar_id,
            "detail": req.detail,
            "stage": stage.db_thing().to_string(),
            "owner": owner.db_thing().to_string(),
            "tx": tx.db_thing().to_string(),
        }))
        .await
        .map_err(mls!(ErrCode::DbError))?;

    let new_avatar: Vec<SurrealRecord> = response.take(1).map_err(mls!(ErrCode::DbError))?;

    if let Some(v) = new_avatar.into_iter().next() {
        if let Some(avatar) =
            Avatar::db_load_by_id(v.id.id.to_raw().parse().unwrap(), &state.db.manager).await?
        {
            tx.set_validate(&state.db.manager).await?;
            return Ok(Json(avatar));
        } else {
            tx.set_invalid(&state.db.manager).await?;
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
