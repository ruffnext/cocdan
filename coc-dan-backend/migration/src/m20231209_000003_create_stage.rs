use sea_orm_migration::{prelude::*, async_trait::async_trait};

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.create_table(Table::create()
            .table(Stage::Table)
            .col(
                ColumnDef::new(Stage::Uuid)
                    .uuid()
                    .unique_key()
                    .not_null()
                    .primary_key()
            )
            .col(
                ColumnDef::new(Stage::Owner)
                    .integer()
                    .not_null()
            )
            .col(
                ColumnDef::new(Stage::Title)
                    .string()
                    .not_null()
            )
            .col(
                ColumnDef::new(Stage::Description)
                    .string()
                    .not_null()
            )
            .col(
                ColumnDef::new(Stage::GameMap)
                    .json()
                    .not_null()
            )
            .to_owned()
        ).await
    }

    async fn down(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.drop_table(Table::drop().table(Stage::Table).to_owned()).await
    }
}

#[derive(Iden)]
pub enum Stage {
    Table,
    Uuid,
    Owner,
    Title,
    Description,
    GameMap
}