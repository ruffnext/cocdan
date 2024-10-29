use serde::{de::DeserializeOwned, Deserialize, Serialize};
use surrealdb::{
    engine::remote::ws::{Client, Ws},
    opt::auth::Root,
    sql::{Id, Thing},
    RecordIdKey, Surreal,
};
use tracing::info;

use crate::{
    mls,
    typedef::err::{ErrCode, Left},
};

pub mod entities;
#[cfg(test)]
pub mod migration;
pub mod relations;

#[derive(Clone)]
pub struct DbService {
    pub manager: Surreal<Client>,
}

pub type DbConn = Surreal<Client>;

impl DbService {
    #[tracing::instrument(skip(self, binds))]
    pub async fn query_manager_bind<T, R>(&self, query: &str, binds: T) -> Result<Vec<R>, Left>
    where
        T: Serialize + 'static,
        R: DeserializeOwned,
    {
        info!(target : "query_manager_bind", query = ?query, binds = serde_json::to_string(&binds).unwrap_or_default());
        let mut query_result = self
            .manager
            .query(query)
            .bind(binds)
            .await
            .map_err(mls!(ErrCode::DbError))?;

        let records: Vec<R> = query_result.take(0).map_err(mls!(ErrCode::DbError))?;
        Ok(records)
    }

    #[tracing::instrument(skip(self))]
    pub async fn query_manager<R>(&self, query: &str) -> Result<Vec<R>, Left>
    where
        R: DeserializeOwned,
    {
        info!(target : "query_manager", query = ?query);
        let mut query_result = self
            .manager
            .query(query)
            .await
            .map_err(mls!(ErrCode::DbError))?;

        let records: Vec<R> = query_result.take(0).map_err(mls!(ErrCode::DbError))?;
        Ok(records)
    }

    pub async fn new() -> Result<Self, Left> {
        let manager = get_db("manager").await?;
        Ok(Self { manager })
    }
}

#[allow(unused)]
#[derive(Deserialize, Debug)]
pub struct SurrealRecord {
    pub id: Thing,
}

pub trait DbEntity
where
    Self: Serialize + DeserializeOwned + Sized + Clone + 'static,
{
    fn db_id(&self) -> Id;
    fn db_thing(&self) -> Thing {
        Thing::from((Self::db_tab_name().to_string(), self.db_id()))
    }
    fn db_tab_name() -> &'static str;

    async fn db_load_by_id(id: Id, db: &DbConn) -> Result<Option<Self>, Left> {
        let r: Option<Self> = db
            .select((Self::db_tab_name(), RecordIdKey::from_inner(id)))
            .await
            .map_err(mls!(ErrCode::DbError))?;
        Ok(r)
    }

    #[tracing::instrument(skip(self, db))]
    async fn db_save(&self, db: &DbConn) -> Result<(), Left> {
        info!("{}", serde_json::to_string(&self).unwrap());
        let x: Option<SurrealRecord> = db
            .upsert((Self::db_tab_name(), RecordIdKey::from_inner(self.db_id())))
            .content(self.clone())
            .await
            .map_err(mls!(ErrCode::DbError))?;
        info!("surreal_save: {:?}", x);
        Ok(())
    }

    #[tracing::instrument(skip(db))]
    async fn db_del(id: Id, db: &DbConn) -> Result<(), Left> {
        let _: Option<SurrealRecord> = db
            .delete((Self::db_tab_name(), RecordIdKey::from_inner(id)))
            .await
            .map_err(mls!(ErrCode::DbError))?;
        Ok(())
    }
}

pub async fn get_db(db_name: &str) -> Result<DbConn, Left> {
    let url = std::env::var("SURREAL_ENDPOINT").expect("SURREAL_URL must be set");
    let user = std::env::var("SURREAL_USER").expect("SURREAL_USER must be set");
    let pass = std::env::var("SURREAL_PASS").expect("SURREAL_PASS must be set");

    let db = Surreal::new::<Ws>(url)
        .await
        .map_err(mls!(ErrCode::DbError))?;

    db.signin(Root {
        username: &user,
        password: &pass,
    })
    .await
    .map_err(mls!(ErrCode::DbError))?;

    let surreal_ns = std::env::var("SURREAL_NS").expect("SURREAL_NS must be set");

    // Select a specific namespace / database
    db.use_ns(surreal_ns)
        .use_db(db_name)
        .await
        .map_err(mls!(ErrCode::DbError))?;

    Ok(db)
}

#[cfg(test)]
pub async fn new_mock_db() -> DbService {
    use migration::{drop_surreal, init_surreal};

    std::env::set_var("SURREAL_NS", "test");
    let db = DbService::new().await.expect("failed to connect to db");
    drop_surreal(&db.manager).await.expect("failed to drop db");
    init_surreal(&db.manager).await;
    db
}
