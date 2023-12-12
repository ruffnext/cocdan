use sea_orm_migration::{prelude::*, async_trait::async_trait};

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.create_table(Table::create()
            .table(LinkStageUser::Table)
            .col(
                ColumnDef::new(LinkStageUser::Id)
                    .integer()
                    .not_null()
                    .primary_key()
            )
            .col(
                ColumnDef::new(LinkStageUser::StageId)
                    .uuid()
                    .not_null()
            )
            .col(
                ColumnDef::new(LinkStageUser::UserId)
                    .integer()
                    .not_null()
            )
            .to_owned()
        ).await?;

        Ok(())
    }

    async fn down(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.drop_table(Table::drop().table(LinkStageUser::Table).to_owned()).await
    }
}

#[derive(Iden)]
pub enum LinkStageUser {
    Table,
    Id,
    UserId,
    StageId
}
