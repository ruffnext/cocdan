use serde::{Deserialize, Serialize};
mod m241028_init;

use crate::{
    daemon::DbService,
    mls,
    typedef::err::{ErrCode, Left},
};

use super::{DbConn, SurrealRecord};

#[async_trait::async_trait]
pub(super) trait MigrationTrait
where
    Self: Send + Sync,
{
    async fn setup(&self, db: &DbConn) -> Result<(), surrealdb::Error>;
    async fn destroy(&self, db: &DbConn) -> Result<(), surrealdb::Error>;
    fn version(&self) -> &'static str;
}

fn get_migrations() -> Vec<Box<dyn MigrationTrait>> {
    vec![Box::new(m241028_init::M241028Init {})]
}

pub async fn drop_surreal(db: &DbConn) -> Result<(), Left> {
    let drop_db = "
        REMOVE TABLE _migration;
    ";

    db.query(drop_db).await.map_err(mls!(ErrCode::DbError))?;
    for m in get_migrations() {
        m.destroy(db).await.map_err(mls!(ErrCode::DbError))?;
    }

    Ok(())
}

pub async fn init_surreal(db: &DbConn) {
    let create_db = "
        DEFINE TABLE IF NOT EXISTS _migration SCHEMAFULL;
        DEFINE FIELD IF NOT EXISTS applied_at ON _migration TYPE datetime;
    ";

    db.query(create_db).await.expect("failed to query");

    for m in get_migrations() {
        apply(db, &m).await.expect("failed to apply migration");
    }
}

#[derive(Serialize, Deserialize)]
struct MigrateRecord {
    applied_at: surrealdb_core::sql::Datetime,
}

async fn apply(db: &DbConn, m: &Box<dyn MigrationTrait>) -> Result<(), Left> {
    let version = m.version();
    let r: Option<SurrealRecord> = db
        .select(("_migration", version))
        .await
        .map_err(mls!(ErrCode::DbError))?;

    if let Some(v) = r {
        if v.id.to_raw() == version {
            println!("migration {} already applied", version);
            return Ok(());
        }
    } else {
        println!("migration {} not applied", version);
    }

    m.setup(db).await.map_err(mls!(ErrCode::DbError))?;

    let record = MigrateRecord {
        applied_at: surrealdb_core::sql::Datetime::from(chrono::Utc::now()),
    };

    let _: Option<MigrateRecord> = db
        .upsert(("_migration", version))
        .content(record)
        .await
        .map_err(mls!(ErrCode::DbError))?;

    Ok(())
}

#[tokio::test]
#[ignore]
pub async fn migrate() {
    dotenvy::dotenv().ok();
    let db = DbService::new().await.unwrap();
    init_surreal(&db.manager).await;
}

#[tokio::test]
#[ignore]
pub async fn reset_surreal() {
    dotenvy::dotenv().ok();
    let db = DbService::new().await.unwrap();
    drop_surreal(&db.manager)
        .await
        .expect("failed to drop surreal");
    init_surreal(&db.manager).await;
}
