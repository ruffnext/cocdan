use sea_orm_migration::{prelude::*, async_trait::async_trait};

use super::m20231209_000001_create_user_table::User;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.create_table(Table::create()
            .table(Session::Table)
            .col(
                ColumnDef::new(Session::Uuid)
                    .uuid()
                    .unique_key()
                    .not_null()
                    .primary_key()
            )
            .col(
                ColumnDef::new(Session::UserId)
                    .integer()
                    .not_null()
            ).foreign_key(
                ForeignKey::create()
                    .name("fk-session-user_id")
                    .on_delete(ForeignKeyAction::Cascade)
                    .from(Session::Table, Session::UserId)
                    .to(User::Table, User::Id)
            )
            .to_owned()
        ).await
    }

    async fn down(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.drop_table(Table::drop().table(Session::Table).to_owned()).await
    }
}

#[derive(Iden)]
pub enum Session {
    Table,
    Uuid,
    UserId
}