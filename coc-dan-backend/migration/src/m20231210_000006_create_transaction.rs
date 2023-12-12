use sea_orm_migration::{prelude::*, async_trait::async_trait};

use crate::m20231209_000003_create_stage::Stage;

use super::m20231209_000001_create_user_table::User;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.create_table(Table::create()
            .table(Transaction::Table)
            .col(
                ColumnDef::new(Transaction::Id)
                    .integer()
                    .not_null()
                    .primary_key()
                    .auto_increment()
            )
            .col(
                ColumnDef::new(Transaction::StageUuid)
                    .uuid()
                    .not_null()
            )
            .col(
                ColumnDef::new(Transaction::UserId)
                    .integer()
                    .not_null()
            )
            .foreign_key(
                ForeignKey::create()
                    .name("fk-tx-user_id")
                    .from(Transaction::Table, Transaction::UserId)
                    .to(User::Table, User::Id)
            )
            .foreign_key(
                ForeignKey::create()
                    .name("fk-tx-stage_uuid")
                    .from(Transaction::Table, Transaction::StageUuid)
                    .to(Stage::Table, Stage::Uuid)
            )
            .to_owned()
        ).await?;

        Ok(())
    }

    async fn down(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.drop_table(Table::drop().table(Transaction::Table).to_owned()).await
    }
}

#[derive(Iden)]
pub enum Transaction {
    Table,
    Id,
    StageUuid,
    UserId,
}