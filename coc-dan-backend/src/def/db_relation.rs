use crate::entities::*;
use sea_orm::{Related, RelationDef, EntityTrait};

impl Related<stage::Entity> for link_stage_user::Entity {
    fn to() -> RelationDef {
        link_stage_user::Entity::belongs_to(stage::Entity)
            .from(link_stage_user::Column::StageId)
            .to(stage::Column::Uuid)
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
