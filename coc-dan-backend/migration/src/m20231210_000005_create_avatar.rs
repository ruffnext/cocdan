use sea_orm_migration::{prelude::*, async_trait::async_trait};

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.create_table(Table::create()
            .table(Avatar::Table)
            .col(
                ColumnDef::new(Avatar::Id)
                    .integer()
                    .not_null()
                    .primary_key()
                    .auto_increment()
            )
            .col(
                ColumnDef::new(Avatar::StageUuid)
                    .uuid()
            )
            .col(
                ColumnDef::new(Avatar::Owner)
                    .integer()
                    .not_null()
            )
            .col(
                ColumnDef::new(Avatar::Name)
                    .string()
                    .not_null()
            )
            .col(ColumnDef::new(Avatar::Header).string())
            .col(ColumnDef::new(Avatar::Detail).json_binary().not_null())
            .to_owned()
        ).await?;

        Ok(())
    }

    async fn down(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.drop_table(Table::drop().table(Avatar::Table).to_owned()).await
    }
}

#[derive(Iden)]
pub enum Avatar {
    Table,
    Id,
    StageUuid,
    Owner,
    Name,
    Header,    // binary image
    Detail,
}