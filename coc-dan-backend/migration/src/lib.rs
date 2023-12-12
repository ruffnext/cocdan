pub mod m20231209_000001_create_user_table;
pub mod m20231209_000002_create_session;
pub mod m20231209_000003_create_stage;
pub mod m20231210_000004_create_relation_stage_user;
pub mod m20231210_000005_create_avatar;
pub mod m20231210_000006_create_transaction;

use sea_orm_migration::prelude::*;

pub struct Migrator;

#[async_trait::async_trait]
impl MigratorTrait for Migrator {
    fn migrations() -> Vec<Box<dyn MigrationTrait>> {
        vec![
            Box::new(m20231209_000001_create_user_table::Migration),
            Box::new(m20231209_000002_create_session::Migration),
            Box::new(m20231209_000003_create_stage::Migration),
            Box::new(m20231210_000004_create_relation_stage_user::Migration),
            Box::new(m20231210_000005_create_avatar::Migration),
            Box::new(m20231210_000006_create_transaction::Migration)
        ]
    }
}
