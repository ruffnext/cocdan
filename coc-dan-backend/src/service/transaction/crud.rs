use coc_dan_common::def::transaction::{Tx, service::IQueryStageTxs, ITransaction};
use sea_orm::{EntityTrait, ColumnTrait, DbErr, ActiveValue, ActiveModelTrait, QueryFilter, ConnectionTrait};
use crate::{entities::*, AppState, service::{stage::StageUser, avatar::UserControlledAvatar}, err::Left};
use axum::{extract::{State, Query}, response::{IntoResponse, Response}, Json};

use super::realtime_tx::{get_state_by_stage_id, step_state};

pub async fn add_tx<T : ConnectionTrait>(
    stage_id : i32, 
    user_id : i32, 
    avatar_id : i32, 
    tx : &Tx, 
    ctx : &T
) -> Result<ITransaction, DbErr> {
    let last_state = get_state_by_stage_id(stage_id, ctx).await.map_err(|x| {
        DbErr::Custom(x.uuid.to_string())
    })?;

    let res : ITransaction = transaction::ActiveModel {
        tx_id : ActiveValue::Set(last_state.last_tx.tx_id as i32 + 1),
        stage_id : ActiveValue::Set(stage_id),
        user_id : ActiveValue::Set(user_id),
        time : ActiveValue::Set(chrono::Local::now().to_rfc3339()),
        tx : ActiveValue::Set(serde_json::to_string(tx).unwrap()),
        avatar_id : ActiveValue::Set(avatar_id),
        ..Default::default()
    }.insert(ctx).await?.into();

    step_state(stage_id, last_state, &res).await;
    
    Ok(res)
}

pub async fn query_stage_txs(
    stage_user : StageUser,
    Query(params) : Query<IQueryStageTxs>,
    State(state) : State<AppState>
) -> Result<impl IntoResponse, Left> {
    let mut query = transaction::Entity::find()
        .filter(transaction::Column::StageId.eq(stage_user.stage.id));
    match params.begin {
        Some(v) => {
            query = query.filter(transaction::Column::TxId.gte(v))
        },
        None => {}
    }
    match params.end {
        Some(v) => {
            query = query.filter(transaction::Column::TxId.lte(v))
        },
        None => {}
    }

    let res = query.all(&state.db).await?;
    if res.len() == 0 {
        Err(Left {
            status : http::StatusCode::INTERNAL_SERVER_ERROR,
            message : format!("Stage {} has no transaction", stage_user.stage.id),
            uuid : "64f4e811"
        })?
    }
    let res : Vec<ITransaction> = res.into_iter().map(|x| x.into()).collect();
    Ok((http::StatusCode::OK, Json(res)))
}

pub async fn action_service (
    user_avatar : UserControlledAvatar,
    State(state) : State<AppState>,
    Json(tx): Json<Tx>
) -> Result<Response, Left> {
    let stage_id = match user_avatar.avatar.stage_id {
        Some(v) => v,
        None => {
            return Err(Left {
                status : http::StatusCode::BAD_REQUEST,
                message : format!("Avatar {} is not on a stage, no transaction needed", user_avatar.avatar.id),
                uuid : "3ec0cdef"
            })
        }
    };
    let res = match tx {
        tx @ _ => {
            add_tx(
                stage_id, 
                user_avatar.user.id, 
                user_avatar.avatar.id, 
                &tx, 
                &state.db
            ).await?
        }
    };

    Ok((http::StatusCode::OK, Json(res)).into_response())
}

pub async fn query_stage_realtime_state (
    stage_user : StageUser,
    State(state) : State<AppState>,
) -> Result<Response, Left> {
    let res = get_state_by_stage_id(stage_user.stage.id, &state.db).await?;
    Ok((http::StatusCode::OK, Json(res)).into_response())
}
