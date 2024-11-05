use super::MigrationTrait;

pub struct M241028Init {}

#[async_trait::async_trait]
impl MigrationTrait for M241028Init {
    fn version(&self) -> &'static str {
        "m241028_init"
    }

    async fn setup(&self, db: &super::DbConn) -> Result<(), surrealdb::Error> {
        let create_db = r#"
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


        DEFINE TABLE stage SCHEMAFULL;
        DEFINE FIELD raw_id ON TABLE stage TYPE int;
        DEFINE FIELD title ON TABLE stage TYPE string;
        DEFINE FIELD description ON TABLE stage TYPE string DEFAULT "";
        DEFINE FIELD rule ON TABLE stage TYPE string;
        DEFINE FIELD owner ON TABLE stage TYPE record<user>;
        DEFINE INDEX rawIdIdx ON TABLE stage COLUMNS raw_id UNIQUE;

        DEFINE TABLE avatar SCHEMAFULL;
        DEFINE FIELD raw_id ON TABLE avatar TYPE string;
        DEFINE FIELD name ON TABLE avatar TYPE string;
        DEFINE FIELD detail ON TABLE avatar TYPE object FLEXIBLE;
        DEFINE FIELD stage ON TABLE avatar TYPE record<stage>;
        DEFINE FIELD owner ON TABLE avatar TYPE record<user>;
        DEFINE FIELD creation_time ON TABLE avatar TYPE datetime DEFAULT time::now();
        DEFINE FIELD last_update_time ON TABLE avatar TYPE datetime DEFAULT time::now();
        DEFINE FIELD header ON TABLE avatar TYPE string DEFAULT "";
        DEFINE INDEX rawIdIdx ON TABLE avatar COLUMNS raw_id UNIQUE;
        DEFINE INDEX ownerIdIdx ON TABLE avatar COLUMNS owner;
        DEFINE INDEX stageIdIdx ON TABLE avatar COLUMNS stage;

        DEFINE TABLE r_user_join_stage TYPE RELATION IN user OUT stage ENFORCED;

        DEFINE TABLE tx SCHEMAFULL;
        DEFINE FIELD raw_id ON TABLE tx TYPE int;
        DEFINE FIELD tx_id ON TABLE tx TYPE int;
        DEFINE FIELD stage ON TABLE tx TYPE record<stage>;
        DEFINE FIELD user ON TABLE tx TYPE record<user>;
        DEFINE FIELD avatar ON TABLE tx TYPE record<avatar>;
        DEFINE FIELD time ON TABLE tx TYPE datetime DEFAULT time::now();
        DEFINE FIELD action ON TABLE tx TYPE object FLEXIBLE;
        DEFINE INDEX txIdIdx ON TABLE tx COLUMNS tx_id;
        DEFINE INDEX stageIdIdx ON TABLE tx COLUMNS stage;
        DEFINE INDEX timeIdx ON TABLE tx COLUMNS time;
        DEFINE INDEX rawIdIdx ON TABLE tx COLUMNS raw_id UNIQUE;
        DEFINE INDEX userIdIdx ON TABLE tx COLUMNS user;
        DEFINE INDEX avatarIdIdx ON TABLE tx COLUMNS avatar;

        COMMIT TRANSACTION;
        "#;

        db.query(create_db).await?;

        Ok(())
    }

    async fn destroy(&self, db: &super::DbConn) -> Result<(), surrealdb::Error> {
        let drop_db = "
            REMOVE TABLE session;
            REMOVE TABLE user;
            REMOVE TABLE stage;
            REMOVE TABLE avatar;
            REMOVE TABLE r_user_join_stage;
            REMOVE TABLE tx;
        ";

        db.query(drop_db).await?;

        Ok(())
    }
}
