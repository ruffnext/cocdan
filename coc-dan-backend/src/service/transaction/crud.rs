use coc_dan_common::def::transaction::{Tx, service::IQueryStageTxs, ITransaction};
use sea_orm::{EntityTrait, ColumnTrait, QueryFilter};
use crate::{entities::*, AppState, service::{stage::StageUser, avatar::UserControlledAvatar}, err::Left};
use axum::{extract::{State, Query}, response::{IntoResponse, Response}, Json};

use super::realtime_tx::{lock_stage_state, perform_tx};


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
            let lock = lock_stage_state(stage_id, &state.db).await?;
            let mut state_lock = lock.get(&stage_id).unwrap().write().await;
            perform_tx((&mut state_lock, stage_id), &state.db, user_avatar.user.id, user_avatar.avatar.id, &tx).await?;
        }
    };

    Ok((http::StatusCode::OK, Json(res)).into_response())
}

pub async fn query_stage_realtime_state (
    stage_user : StageUser,
    State(state) : State<AppState>,
) -> Result<Response, Left> {
    let lock = lock_stage_state(stage_user.stage.id, &state.db).await?;
    let res = lock.get(&stage_user.stage.id).unwrap().read().await.clone();
    Ok((http::StatusCode::OK, Json(res)).into_response())
}




