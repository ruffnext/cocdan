use sea_orm::{DatabaseConnection, Database};

pub async fn get_db() -> DatabaseConnection {
    Database::connect(std::env::var("DATABASE_URL").unwrap()).await.unwrap()
}

#[cfg(test)] 
pub mod tests {

    use sea_orm::{Database, DatabaseConnection};
    use migration::*;
    use sea_orm_migration::prelude::*;

    pub async fn new_mock_db() -> DatabaseConnection {
        let raw_db = Database::connect("sqlite::memory:").await.unwrap();
        Migrator::up(&raw_db, None).await.unwrap();
        raw_db
    }
}
