use axum::{
    extract::{Path, State},
    response::{IntoResponse, Response},
    Json,
};
use serde::Deserialize;
use serde_json::json;

use crate::{
    daemon::{
        entities::{Avatar, RolePlay, Session, SessionType, Stage, TxAction, TxAux},
        DbEntity,
    },
    left_span,
    typedef::err::{ErrCode, Left},
    AppState,
};

#[derive(Deserialize, ts_rs::TS)]
#[ts(
    export,
    export_to = "api/tx/role_play/IReqRolePlay.d.ts",
    rename = "IReqRolePlay"
)]
pub struct IReqRolePlay {
    avatar_id: String,
    text: String,
}

pub async fn role_play(
    State(state): State<AppState>,
    session: Session,
    Path(stage_id): Path<String>,
    Json(req): Json<IReqRolePlay>,
) -> Result<Response, Left> {
    let user = match session.session_type {
        SessionType::User(user) => user,
    };

    let stage =
        if let Some(stage) = Stage::db_load_by_id(stage_id.clone(), &state.db.manager).await? {
            stage
        } else {
            return Err(left_span!(ErrCode::InvalidParameter(
                format!("Stage not found: {}", stage_id).into()
            )));
        };

    let avatar = if let Some(avatar) =
        Avatar::db_load_by_id(req.avatar_id.clone(), &state.db.manager).await?
    {
        avatar
    } else {
        return Err(left_span!(ErrCode::InvalidParameter(
            format!("Avatar not found: {}", req.avatar_id).into()
        )));
    };

    if user.raw_id != avatar.owner.raw_id {
        return Err(left_span!(ErrCode::PermissionDenied(
            "You have no access to role play with this avatar".into()
        )));
    }

    if avatar.stage.raw_id != stage.raw_id {
        return Err(left_span!(ErrCode::PermissionDenied(
            "You have no access to role play with this avatar".into()
        )));
    }

    let mut tx = TxAux::new(
        stage.raw_id,
        user.raw_id,
        avatar.raw_id,
        TxAction::RolePlay(RolePlay {
            text: req.text.clone(),
        }),
        &state.db.manager,
    )
    .await?;

    tx.set_validate(&state.db.manager).await?;

    Ok(Json(json!({
        "message": "Role play success",
    }))
    .into_response())
}
