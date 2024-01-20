use axum::extract::{Path, State};
use axum::response::{Response, IntoResponse};
use axum::{Json, extract};
use coc_dan_common::def::avatar::service::ICreateAvatar;
use coc_dan_common::def::transaction::{Tx, ITransaction};
use coc_dan_common::def::user::IUser;
use sea_orm::{EntityTrait, QueryFilter, ColumnTrait, ActiveValue, ActiveModelTrait, IntoActiveModel, DbErr, TransactionTrait, PaginatorTrait, QuerySelect, ConnectionTrait};
use crate::AppState;
use crate::entities::{prelude::*, *};
use crate::err::Left;
use crate::service::transaction::realtime_tx::{lock_stage_state, perform_tx};
use coc_dan_common::def::avatar::IAvatar;

use super::UserControlledAvatar;

pub async fn list_by_user(
    u : user::Model,
    State(state) : State<AppState>
) -> Result<Json<Vec<IAvatar>>, Left> {
    let db = state.db;
    let raw = Avatar::find().filter(crate::entities::avatar::Column::Owner.eq(u.id)).all(&db).await?;
    if raw.len() > 0 {
        Ok(Json(raw.into_iter().map(|x| x.into()).collect()))
    } else {
        Err(Left { 
            status: http::StatusCode::NO_CONTENT, 
            message: String::new(),
            uuid: "98f4ac40" 
        })
    }
}

pub async fn get_avatar_by_id (
    u : user::Model, 
    Path(id) : Path<i32>,
    State(state) : State<AppState>
) -> Result<avatar::Model, Left> {
    match Avatar::find_by_id(id).one(&state.db).await? {
        Some(v) if v.owner == u.id => {
            Ok(v)
        }, 
        _ => {
            Err(Left { 
                status: http::StatusCode::NO_CONTENT, 
                message: String::new(), 
                uuid: "d0241afa" 
            })
        }
    }
}

pub async fn get_by_id_req (
    u : user::Model, 
    Path(id) : Path<i32>,
    State(state) : State<AppState>
) -> Result<Response, Left> {
    Ok((http::StatusCode::OK, Json(get_avatar_by_id(u, Path(id), State(state)).await?)).into_response())
}

pub async fn create (
    u : crate::entities::user::Model, 
    State(state) : State<AppState>,
    extract::Json(params) : extract::Json<ICreateAvatar>
) -> Result<Json<IAvatar>, Left> {
    let db = &state.db;

    match params.stage_id {
        Some(stage_uuid) => {
            match LinkStageUser::find()
                .filter(link_stage_user::Column::StageId.eq(stage_uuid.to_string()))
                .filter(link_stage_user::Column::UserId.eq(u.id))
                .one(db).await?
                {
                    None => {
                        return Err(Left { 
                            status: http::StatusCode::UNAUTHORIZED, 
                            message: format!("You are not a member of stage {}", stage_uuid), 
                            uuid: "4e1da279" 
                        })
                    }
                    _ => {}
                }
        },
        None => {}
    };


    let res = db.transaction::<_, IAvatar, DbErr>(|ctx| {
        Box::pin(async move {
            let res : IAvatar = avatar::ActiveModel {
                stage_id : ActiveValue::Set(params.stage_id),
                owner : ActiveValue::Set(u.id),
                name : ActiveValue::Set(params.name),
                detail : ActiveValue::Set(serde_json::to_string(&params.detail.unwrap_or_default()).unwrap()),
                header : ActiveValue::Set(None),
                ..Default::default()
            }.insert(ctx).await?.into();
            match params.stage_id {
                Some(v) => {
                    let lock = lock_stage_state(v, ctx).await?;
                    let mut state = lock.get(&v).unwrap().write().await;
                    let tx : Tx = Tx::UpdateAvatar { before: None, after: Some(res.clone()) };
                    perform_tx((&mut state, v), ctx, u.id, res.id, &tx).await?;
                },
                _ => {}
            };
            Ok(res)
        })
    }).await?;


    Ok(Json(res.into()))
}

pub async fn destroy (
    user_avatar : UserControlledAvatar,
    State(state) : State<AppState>
) -> Result<http::StatusCode, Left> {
    state.db.transaction::<_, (), DbErr>(|ctx| {
        Box::pin(async move {
            match user_avatar.avatar.stage_id {
                Some(v) => {
                    let lock = lock_stage_state(v, ctx).await?;
                    let mut state = lock.get(&v).unwrap().write().await;
                    let avatar_id = user_avatar.avatar.id;
                    Avatar::delete_by_id(avatar_id).exec(ctx).await?;
                    let tx = Tx::UpdateAvatar { before: Some(user_avatar.avatar.into()), after: None };
                    perform_tx((&mut state, v), ctx, user_avatar.user.id, avatar_id, &tx).await?;
                },
                None => {
                    Avatar::delete_by_id(user_avatar.avatar.id).exec(ctx).await?;
                }
            };
            Ok(())
        })
    }).await?;
    Ok(http::StatusCode::OK)
}

pub async fn update_avatar (
    user_avatar : UserControlledAvatar,
    State(state) : State<AppState>,
    Json(a): Json<IAvatar>
) -> Result<Response, Left> {
    if a.id != user_avatar.avatar.id {
        return Err(Left {
            status : http::StatusCode::BAD_REQUEST,
            message : "Modification of avatar id is prohibited".to_string(),
            uuid : "c93414a5"
        })
    }
    
    state.db.transaction::<_, (), DbErr>(|ctx| {
        let a = a.clone();
        Box::pin(async move {
            update_avatar_inner(user_avatar.user.into(), user_avatar.avatar.into(), a.into(), ctx).await?;
            Ok(())
        })
    }).await?;
 
    Ok((http::StatusCode::OK, Json(a)).into_response())
}

pub async fn update_avatar_inner<T : ConnectionTrait> (
    user : IUser,
    avatar_before : IAvatar,
    avatar_after  : IAvatar,
    ctx : &T
) -> Result<ITransaction, DbErr> {

    if avatar_before.owner != avatar_after.owner {
        if User::find().select_only()
            .column(user::Column::Id)
            .filter(user::Column::Id.eq(avatar_after.owner))
            .count(ctx).await? == 1 {
            return Err(DbErr::Custom("289ef556".to_string()))
        }
    }

    if avatar_before.stage_id != avatar_after.stage_id {
        if LinkStageUser::find()
            .select_only()
            .column(link_stage_user::Column::Id)
            .filter(link_stage_user::Column::UserId.eq(avatar_before.owner))
            .filter(link_stage_user::Column::StageId.eq(avatar_after.stage_id))
            .count(ctx).await? == 1 {
           return Err(DbErr::Custom("fafe2381".to_string())) 
        }
    }

    let model : avatar::Model = avatar_after.clone().into();
    let active_model : avatar::ActiveModel = model.into_active_model();
    let avatar_id = avatar_after.id;
    let res_tx = match (avatar_before.stage_id, avatar_after.stage_id) {
        (None, None) => {
            Avatar::update(active_model).exec(ctx).await?;
            ITransaction {
                tx_id : 0,
                stage_id : 0,
                user_id : user.id,
                avatar_id ,
                time : chrono::Local::now().to_rfc3339(),
                tx : Tx::UpdateAvatar { before: Some(avatar_before), after: Some(avatar_after) }
            }
        },
        (Some(a), Some(b)) => {
            if a != b {
                return Err(DbErr::Custom("Not supported yet.".to_string()))
            } else {
                let lock = lock_stage_state(a, ctx).await?;
                let mut state = lock.get(&a).unwrap().write().await;
                Avatar::update(active_model).exec(ctx).await?;
                let tx = Tx::UpdateAvatar { before: Some(avatar_before), after: Some(avatar_after) };
                perform_tx((&mut state, a), ctx, user.id, avatar_id, &tx).await?;
                state.last_tx.clone()
            }
        },
        (Some(before), None) => {
            let lock = lock_stage_state(before, ctx).await?;
            Avatar::update(active_model).exec(ctx).await?;
            let mut state = lock.get(&before).unwrap().write().await;
            let tx = Tx::UpdateAvatar { before: Some(avatar_before), after: None };
            perform_tx((&mut state, before), ctx, user.id, avatar_id, &tx).await?;
            state.last_tx.to_owned()
        },
        (None, Some(after)) => {
            let lock = lock_stage_state(after, ctx).await?;
            Avatar::update(active_model).exec(ctx).await?;
            let mut state = lock.get(&after).unwrap().write().await;
            let tx = Tx::UpdateAvatar { before: None, after: Some(avatar_after) };
            perform_tx((&mut state, after), ctx, user.id, avatar_id, &tx).await?;
            state.last_tx.to_owned()
        }
    };

    Ok(res_tx)
}
