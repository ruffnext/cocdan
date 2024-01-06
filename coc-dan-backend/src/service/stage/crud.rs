use axum::extract::{Path, State};
use axum::response::{IntoResponse, Response};
use axum::{Json, extract};
use coc_dan_common::def::GameMap;
use sea_orm::{EntityTrait, QueryFilter, ColumnTrait, ActiveModelTrait, DbErr, ActiveValue, DatabaseConnection, IntoActiveModel };
use tracing::debug;
use uuid::Uuid;
use sea_orm::TransactionTrait;
use coc_dan_common::def::user::IUser;
use coc_dan_common::def::stage::service::ICreateStage;

use crate::AppState;
use crate::service::avatar::clear_user_stage_avatars;
use crate::err::Left;
use crate::entities::{prelude::*, *};

use super::IStage;

pub async fn create (
    u : crate::entities::user::Model, 
    State(state) : State<AppState>,
    extract::Json(params) : extract::Json<ICreateStage>
) -> Result<Json<IStage>, Left> {
    let game_map = GameMap::new_empty();
    let db = &state.db;
    let res = db.transaction::<_, stage::Model, DbErr>(|ctx| {
        Box::pin(async move {
            let res = crate::entities::stage::ActiveModel {
                uuid : ActiveValue::Set(Uuid::new_v4().to_string()),
                owner : ActiveValue::Set(u.id),
                title : ActiveValue::Set(params.title.clone()),
                description : ActiveValue::Set(params.description.clone()),
                game_map : ActiveValue::Set(serde_json::to_string(&game_map).unwrap())
            }.insert(ctx).await?;
            
            link_stage_user::ActiveModel {
                stage_id : ActiveValue::Set(res.uuid.clone()),
                user_id : ActiveValue::Set(u.id),
                ..Default::default()
            }.insert(ctx).await?;
            Ok(res)
        })
    }).await?;
    Ok(Json(res.into()))
}

pub async fn get_by_uuid (
    _u : user::Model, 
    Path(uuid) : Path<Uuid>,
    State(state) : State<AppState>
) -> Result<Response, Left> {
    match Stage::find_by_id(uuid.to_string()).one(&state.db).await? {
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
    Path(uuid) : Path<Uuid>,
    State(state) : State<AppState>
) -> Result<Json<Vec<IUser>>, Left> {
    let db = &state.db;
    
    let raw = LinkStageUser::find()
        .filter(link_stage_user::Column::StageId.eq(uuid.to_string()))
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
    uuid : Uuid, 
    db : &DatabaseConnection
) -> Result<IStage, Left> {
    match Stage::find_by_id(uuid.to_string()).one(db).await? {
        Some(v) => Ok(v.into()),
        None => {
            debug!("User {} try to get stage {uuid}, but it does not exist", u.id);
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
    Path(uuid) : Path<Uuid>,
    State(state) : State<AppState>
) -> Result<impl IntoResponse, Left> {
    let db = &state.db;
    let s = query_stage_by_id(&u, uuid, db).await?;

    match LinkStageUser::find()
        .filter(link_stage_user::Column::StageId.eq(uuid.to_string()))
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
                stage_id : ActiveValue::Set(s.uuid),
                user_id : ActiveValue::Set(u.id),
                ..Default::default()
            }.insert(db).await?;
        }
    }
    Ok(http::StatusCode::OK)
}

pub async fn leave_stage (
    u : user::Model,
    Path(uuid) : Path<Uuid>,
    State(state) : State<AppState>
) -> Result<http::StatusCode, Left> {
    let db = &state.db;
    match LinkStageUser::find()
        .filter(link_stage_user::Column::StageId.eq(uuid.to_string()))
        .filter(link_stage_user::Column::UserId.eq(u.id))
        .one(db).await? {
        Some(v) => {
            db.transaction::<_, (), DbErr>(|ctx| {
                Box::pin(async move {
                    LinkStageUser::delete_many()
                        .filter(link_stage_user::Column::StageId.eq(v.stage_id.clone()))
                        .filter(link_stage_user::Column::UserId.eq(u.id))
                        .exec(ctx).await?;

                    match Stage::find_by_id(v.stage_id.clone()).one(ctx).await? {
                        Some(s) => {
                            if s.owner == u.id {
                                Stage::delete(s.into_active_model()).exec(ctx).await?;
                                Avatar::delete_many().filter(avatar::Column::StageUuid.eq(uuid.to_string())).exec(ctx).await?;
                            }
                        },
                        None => {}
                    };

                    clear_user_stage_avatars(&u, &uuid, ctx).await?;

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
