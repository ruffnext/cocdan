use super::MigrationTrait;

pub struct M241028Init {}

#[async_trait::async_trait]
impl MigrationTrait for M241028Init {
    fn version(&self) -> &'static str {
        "m241028_init"
    }

    async fn setup(&self, db: &super::DbConn) -> Result<(), surrealdb::Error> {
        let create_db = "
        BEGIN TRANSACTION;

        DEFINE TABLE user SCHEMAFULL;
        DEFINE FIELD raw_id ON user TYPE int;
        DEFINE FIELD username ON user TYPE string;
        DEFINE FIELD password ON user TYPE string;
        DEFINE FIELD nickname ON user TYPE string;
        DEFINE FIELD registration_time ON user TYPE datetime DEFAULT time::now();
        DEFINE FIELD active_status ON user TYPE string;
        DEFINE INDEX rawIdIdx ON user COLUMNS raw_id UNIQUE;
        DEFINE INDEX usernameIdx ON user COLUMNS username UNIQUE;

        DEFINE TABLE session SCHEMAFULL;
        DEFINE FIELD raw_id ON TABLE session TYPE string;
        DEFINE FIELD session_type ON TABLE session TYPE {
            User: record<user>
        };
        DEFINE FIELD login_time ON TABLE session TYPE datetime;
        DEFINE FIELD expiration_time ON TABLE session TYPE datetime;
        DEFINE INDEX rawIdIdx ON session COLUMNS raw_id UNIQUE;

        COMMIT TRANSACTION;
        ";

        db.query(create_db).await?;

        Ok(())
    }

    async fn destroy(&self, db: &super::DbConn) -> Result<(), surrealdb::Error> {
        let drop_db = "
            REMOVE TABLE session;
            REMOVE TABLE user;
        ";

        db.query(drop_db).await?;

        Ok(())
    }
}
