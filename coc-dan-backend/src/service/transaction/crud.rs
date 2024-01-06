use coc_dan_common::def::transaction::{Tx, service::IQueryStageTxs, ITransaction};
use sea_orm::{FromQueryResult, EntityTrait, QuerySelect, ColumnTrait, DbErr, ActiveValue, ActiveModelTrait, QueryFilter, ConnectionTrait};
use crate::{entities::*, AppState, service::{stage::StageUser, avatar::UserAvatar}, err::Left};
use axum::{extract::{State, Query}, response::{IntoResponse, Response}, Json};

#[derive(FromQueryResult, Debug)]
struct MaxTxId {
    tx_id : i32
}

pub async fn add_tx<T : ConnectionTrait>(
    stage_uuid : &str, 
    user_id : i32, 
    avatar_id : i32, 
    tx : &Tx, 
    ctx : &T
) -> Result<ITransaction, DbErr> {
    let tmp = transaction::Entity::find()
        .select_only()
        .column_as(transaction::Column::TxId.max(), "tx_id")
        .group_by(transaction::Column::StageUuid)
        .having(transaction::Column::StageUuid.eq(stage_uuid))
        .into_model::<MaxTxId>()
        .one(ctx).await?;
    let tx_id = match tmp {
        Some(v) => v.tx_id + 1,
        None => 1
    };
    let res = transaction::ActiveModel {
        tx_id : ActiveValue::Set(tx_id),
        stage_uuid : ActiveValue::Set(stage_uuid.to_string()),
        user_id : ActiveValue::Set(user_id),
        time : ActiveValue::Set(chrono::Local::now().to_rfc3339()),
        tx : ActiveValue::Set(serde_json::to_string(tx).unwrap()),
        avatar_id : ActiveValue::Set(avatar_id),
        ..Default::default()
    }.insert(ctx).await?;
    Ok(res.into())
}

pub async fn query_stage_txs(
    stage_user : StageUser,
    Query(params) : Query<IQueryStageTxs>,
    State(state) : State<AppState>
) -> Result<impl IntoResponse, Left> {
    let mut query = transaction::Entity::find()
        .filter(transaction::Column::StageUuid.eq(stage_user.stage.uuid.to_string()));
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
            status : http::StatusCode::NO_CONTENT,
            message : "".to_string(),
            uuid : ""
        })?
    }
    let res : Vec<ITransaction> = res.into_iter().map(|x| x.into()).collect();
    Ok((http::StatusCode::OK, Json(res)))
}

pub async fn action (
    user_avatar : UserAvatar,
    State(state) : State<AppState>,
    Json(tx): Json<Tx>
) -> Result<Response, Left> {
    let stage_uuid = match user_avatar.avatar.stage_uuid {
        Some(v) => v,
        None => {
            return Err(Left {
                status : http::StatusCode::BAD_REQUEST,
                message : format!("Avatar {} is not on a stage, no transaction needed", user_avatar.avatar.id),
                uuid : ""
            })
        }
    };
    let res = add_tx(&stage_uuid, user_avatar.user.id, user_avatar.avatar.id, &tx, &state.db).await?;
    Ok((http::StatusCode::OK, Json(res)).into_response())
}
