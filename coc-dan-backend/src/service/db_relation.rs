use crate::database::entities::*;
use sea_orm::{EntityTrait, Related, RelationDef};

impl Related<stage::Entity> for link_stage_user::Entity {
    fn to() -> RelationDef {
        link_stage_user::Entity::belongs_to(stage::Entity)
            .from(link_stage_user::Column::StageId)
            .to(stage::Column::Id)
            .into()
    }
}

impl Related<user::Entity> for link_stage_user::Entity {
    fn to() -> RelationDef {
        link_stage_user::Entity::belongs_to(user::Entity)
            .from(link_stage_user::Column::UserId)
            .to(user::Column::Id)
            .into()
    }
}
