use axum::extract::{Path, State};
use axum::{Json, extract};
use sea_orm::{EntityTrait, QueryFilter, ColumnTrait, ActiveValue, ActiveModelTrait, IntoActiveModel, DbErr, ConnectionTrait, TransactionTrait};
use uuid::Uuid;
use crate::AppState;
use crate::entities::{prelude::*, *};
use crate::err::Left;

use super::IAvatar;

pub async fn list_by_user(
    u : user::Model,
    State(state) : State<AppState>
) -> Result<Json<Vec<crate::entities::avatar::Model>>, Left> {
    let db = state.db;
    let raw = Avatar::find().filter(crate::entities::avatar::Column::Owner.eq(u.id)).all(&db).await?;
    if raw.len() > 0 {
        Ok(Json(raw))
    } else {
        Err(Left { 
            status: http::StatusCode::NO_CONTENT, 
            message: String::new(),
            uuid: "98f4ac40" 
        })
    }
}

pub async fn get_by_id (
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
) -> Result<IAvatar, Left> {
    Ok(get_by_id(u, Path(id), State(state)).await?.into())
}

#[derive(serde::Deserialize, serde::Serialize)]
pub struct CreateAvatar {
    pub name : String,
    pub description : String,
    pub stage_id : Uuid,
}

pub async fn create (
    u : crate::entities::user::Model, 
    State(state) : State<AppState>,
    extract::Json(params) : extract::Json<CreateAvatar>
) -> Result<Json<crate::entities::avatar::Model>, Left> {
    let db = &state.db;
    let a = match LinkStageUser::find()
        .filter(link_stage_user::Column::StageId.eq(params.stage_id.to_string()))
        .filter(link_stage_user::Column::UserId.eq(u.id))
        .one(db).await? 
        {
            Some(v) => {
                let i = avatar::ActiveModel {
                    stage_uuid : ActiveValue::Set(v.stage_id.clone()),
                    owner : ActiveValue::Set(u.id),
                    name : ActiveValue::Set(params.name),
                    description : ActiveValue::Set(params.description),
                    header : ActiveValue::Set(None),
                    ..Default::default()
                };
                i.insert(db).await?
            },
            None => {
                return Err(Left { 
                    status: http::StatusCode::UNAUTHORIZED, 
                    message: format!("You are not a member of stage {}", params.stage_id), 
                    uuid: "4e1da279" 
                })
            }
        };
    Ok(Json(a))
}

pub async fn destroy (
    u : user::Model, 
    Path(id) : Path<i32>,
    State(state) : State<AppState>
) -> Result<http::StatusCode, Left> {
    let db = state.db.clone();
    let a = get_by_id(u.clone(), Path(id), State(state)).await?;
    if a.owner != u.id.clone() {
        return Err(Left { 
            status: http::StatusCode::BAD_REQUEST, 
            message: format!("You have no permission to destroy avatar {}", a.name), 
            uuid: "1b280645" 
        });
    }
    Avatar::delete(a.into_active_model()).exec(&db).await?;
    Ok(http::StatusCode::OK)
}

pub async fn clear_user_stage_avatars<T: TransactionTrait + ConnectionTrait> (
    u : &user::Model, 
    stage_uuid : &Uuid, 
    db : &T
) -> Result<(), DbErr> {
    Avatar::delete_many()
        .filter(avatar::Column::StageUuid.eq(stage_uuid.to_string()))
        .filter(avatar::Column::Owner.eq(u.id))
        .exec(db).await?;
    Ok(())
}
