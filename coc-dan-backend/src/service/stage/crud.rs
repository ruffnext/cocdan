use std::collections::HashMap;

use axum::extract::{Path, State};
use axum::response::{IntoResponse, Response};
use axum::{Json, extract};
use coc_dan_common::def::GameMap;
use coc_dan_common::def::transaction::Tx;
use sea_orm::{EntityTrait, QueryFilter, ColumnTrait, ActiveModelTrait, DbErr, ActiveValue, DatabaseConnection};
use tracing::debug;
use sea_orm::TransactionTrait;
use coc_dan_common::def::user::IUser;
use coc_dan_common::def::stage::service::ICreateStage;

use crate::AppState;
use crate::err::Left;
use crate::entities::{prelude::*, *};
use crate::service::avatar::crud::destroy_avatar_inner;
use crate::service::transaction::realtime_tx::{RealtimeState, get_realtime_state};

use super::IStage;

pub async fn create (
    u : crate::entities::user::Model, 
    State(state) : State<AppState>,
    extract::Json(params) : extract::Json<ICreateStage>
) -> Result<Json<IStage>, Left> {
    let game_map = GameMap::new_empty();
    let db = &state.db;
    let (res_stage, res_tx) = db.transaction::<_, (stage::Model, transaction::Model), DbErr>(|ctx| {
        let game_map = game_map.clone();
        Box::pin(async move {
            let res = crate::entities::stage::ActiveModel {
                owner : ActiveValue::Set(u.id),
                title : ActiveValue::Set(params.title.clone()),
                description : ActiveValue::Set(params.description.clone()),
                game_map : ActiveValue::Set(serde_json::to_string(&game_map).unwrap()),
                ..Default::default()
            }.insert(ctx).await?;

            let stage_id = res.id;
            
            link_stage_user::ActiveModel {
                stage_id : ActiveValue::Set(stage_id),
                user_id : ActiveValue::Set(u.id),
                ..Default::default()
            }.insert(ctx).await?;

            let tx = transaction::ActiveModel {
                tx_id : ActiveValue::Set(1),
                stage_id : ActiveValue::Set(stage_id),
                user_id : ActiveValue::Set(u.id),
                time : ActiveValue::Set(chrono::Local::now().to_rfc3339()),
                tx : ActiveValue::Set(serde_json::to_string(&Tx::Statement("Stage Created".to_string())).unwrap()),
                avatar_id : ActiveValue::Set(0),
                ..Default::default()
            }.insert(ctx).await?;
            Ok((res, tx))
        })
    }).await?;

    let mut write_lock = get_realtime_state().await.write().await;
    write_lock.insert(res_stage.id, RealtimeState {
        last_tx : res_tx.into(),
        avatars : HashMap::new(),
        game_map
    });
    drop(write_lock);

    Ok(Json(res_stage.into()))
}

pub async fn get_by_uuid (
    _u : user::Model, 
    Path(id) : Path<i32>,
    State(state) : State<AppState>
) -> Result<Response, Left> {
    match Stage::find_by_id(id).one(&state.db).await? {
        Some(v) => Ok((http::StatusCode::OK, Json(IStage::from(v))).into_response()),
        None => Err(Left {
            status : http::StatusCode::NO_CONTENT,
            message : String::new(),
            uuid : "c6055c3f"
        })
    }
}

pub async fn list_stages_by_user (
    u : user::Model,
    State(state) : State<AppState>
) -> Result<Json<Vec<IStage>>, Left> {
    let db = &state.db;
    
    let stages = LinkStageUser::find()
        .filter(link_stage_user::Column::UserId.eq(u.id))
        .find_also_related(stage::Entity).all(db).await?;

    let mut res : Vec<IStage> = Vec::with_capacity(stages.len());

    for (_link, item) in stages { 
        match item {
            Some(v) => res.push(v.into()),
            _ => {}
        };
    };

    if res.len() > 0 {
        Ok(Json(res))
    } else {
        Err(Left { 
            status: http::StatusCode::NO_CONTENT, 
            message : String::new(),
            uuid: "d68b3eca" 
        })
    }
}

pub async fn list_users_by_stage (
    _u : user::Model, 
    Path(id) : Path<i32>,
    State(state) : State<AppState>
) -> Result<Json<Vec<IUser>>, Left> {
    let db = &state.db;
    
    let raw = LinkStageUser::find()
        .filter(link_stage_user::Column::StageId.eq(id))
        .find_also_related(user::Entity)
        .all(db).await?;
    
    let mut res : Vec<IUser> = Vec::with_capacity(raw.len());

    for (_, user_item) in raw {
        match user_item {
            Some(v) => res.push(v.into()),
            _ => {}
        }
    };

    if res.len() == 0 {
        Err(Left {
            status : http::StatusCode::NO_CONTENT,
            message : String::new(),
            uuid : "bb026e77"
        })
    } else {
        Ok(Json(res))
    }
}

pub async fn query_stage_by_id (
    u : &user::Model,
    id : i32, 
    db : &DatabaseConnection
) -> Result<IStage, Left> {
    match Stage::find_by_id(id).one(db).await? {
        Some(v) => Ok(v.into()),
        None => {
            debug!("User {} try to get stage {id}, but it does not exist", u.id);
            Err(Left { 
                status: http::StatusCode::NO_CONTENT, 
                message: "This stage does not exist".to_string(), 
                uuid: "d7c167e6"
            })
        }
    }
}

pub async fn join_stage (
    u : user::Model,
    Path(id) : Path<i32>,
    State(state) : State<AppState>
) -> Result<impl IntoResponse, Left> {
    let db = &state.db;
    let s = query_stage_by_id(&u, id, db).await?;

    match LinkStageUser::find()
        .filter(link_stage_user::Column::StageId.eq(id))
        .filter(link_stage_user::Column::UserId.eq(u.id))
        .one(db).await? {
        Some(_v) => {
            return Err(Left { 
                status: http::StatusCode::BAD_REQUEST, 
                message: format!("You have already joined this stage."), 
                uuid: "6352862d" 
            })
        },
        None => {
            link_stage_user::ActiveModel {
                stage_id : ActiveValue::Set(s.id),
                user_id : ActiveValue::Set(u.id),
                ..Default::default()
            }.insert(db).await?;
        }
    }
    Ok(http::StatusCode::OK)
}

pub async fn leave_stage (
    u : user::Model,
    Path(id) : Path<i32>,
    State(state) : State<AppState>
) -> Result<http::StatusCode, Left> {
    let db = &state.db;
    match LinkStageUser::find()
        .filter(link_stage_user::Column::StageId.eq(id))
        .filter(link_stage_user::Column::UserId.eq(u.id))
        .one(db).await? {
        Some(v) => {
            db.transaction::<_, (), DbErr>(|ctx| {
                Box::pin(async move {
                    let s = Stage::find_by_id(id).one(ctx).await?.unwrap();

                    if s.owner == u.id {
                        LinkStageUser::delete_many()
                            .filter(link_stage_user::Column::StageId.eq(v.stage_id.clone()))
                            .exec(ctx).await?;

                        Avatar::delete_many()
                            .filter(avatar::Column::StageId.eq(v.stage_id))
                            .exec(ctx).await?;

                        Transaction::delete_many()
                            .filter(transaction::Column::StageId.eq(v.stage_id))
                            .exec(ctx).await?;

                        Stage::delete_by_id(s.id).exec(ctx).await?;

                        get_realtime_state().await.write().await.remove(&id);
                    } else {
                        LinkStageUser::delete_many()
                            .filter(link_stage_user::Column::StageId.eq(v.stage_id.clone()))
                            .filter(link_stage_user::Column::UserId.eq(u.id))
                            .exec(ctx).await?;

                        let avatars = Avatar::find()
                            .filter(avatar::Column::StageId.eq(v.stage_id))
                            .filter(avatar::Column::Owner.eq(v.user_id))
                            .all(ctx).await?;
                        
                        for a in avatars {
                            destroy_avatar_inner(&a.into(), ctx).await?;
                        }

                    }

                    Ok(())
                })
            }).await?;
            Ok(http::StatusCode::OK)
        },
        None => {
            Err(Left { 
                status: http::StatusCode::BAD_REQUEST, 
                message: format!("You have not joined this stage yet."), 
                uuid: "21fa1f82" 
            })
        }
    }
}
