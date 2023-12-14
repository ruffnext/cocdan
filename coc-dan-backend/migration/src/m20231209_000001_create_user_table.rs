use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.create_table(Table::create()
            .table(User::Table)
            .col(
                ColumnDef::new(User::Id)
                    .integer()
                    .not_null()
                    .auto_increment()
                    .primary_key()
            )
            .col(
                ColumnDef::new(User::Name)
                    .string()
                    .unique_key()
                    .not_null()
            )
            .col(ColumnDef::new(User::NickName).string().not_null())
            .col(ColumnDef::new(User::Header).string())
            .to_owned()
        ).await
    }

    async fn down(&self, manager : &SchemaManager) -> Result<(), DbErr> {
        manager.drop_table(Table::drop().table(User::Table).to_owned()).await
    }
}

#[derive(Iden)]
pub enum User {
    Table,
    Id,
    Name,
    NickName,
    Header
}
