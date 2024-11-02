use surrealdb::{
    sql::{Id, Thing},
    RecordIdKey,
};

use super::super::{DbEntity, SurrealRecord};
use crate::{
    daemon::db::DbConn,
    mls,
    typedef::err::{ErrCode, Left},
};

use super::User;

#[derive(serde::Serialize, serde::Deserialize, Debug, Clone, ts_rs::TS)]
#[ts(export, rename = "IStage", export_to = "entity/basic/IStage.d.ts")]
pub struct Stage {
    pub raw_id: i64,
    pub title: String,
    pub rule: StageRule,
    pub description: String,
    pub owner: User,
}

#[derive(serde::Serialize, serde::Deserialize, Debug, Clone, ts_rs::TS)]
#[ts(
    export,
    rename = "IStageRule",
    export_to = "entity/basic/IStageRule.d.ts"
)]
pub enum StageRule {
    CoC7th,
    CoC6th,
}

#[derive(serde::Serialize)]
struct DbStage {
    pub raw_id: i64,
    pub title: String,
    pub description: String,
    pub owner: Thing,
}

impl From<Stage> for DbStage {
    fn from(stage: Stage) -> Self {
        Self {
            raw_id: stage.raw_id,
            title: stage.title,
            description: stage.description,
            owner: stage.owner.db_thing(),
        }
    }
}

impl DbEntity for Stage {
    fn db_id(&self) -> Id {
        self.raw_id.into()
    }

    fn db_tab_name() -> &'static str {
        "stage"
    }

    async fn db_load_by_id(id: Id, db: &DbConn) -> Result<Option<Self>, Left> {
        let query = "SELECT * FROM $id FETCH owner";
        let mut response = db
            .query(query)
            .bind(("id", Thing::from((Self::db_tab_name(), id))))
            .await
            .map_err(mls!(ErrCode::DbError))?;
        let records: Vec<Self> = response.take(0).map_err(mls!(ErrCode::DbError))?;
        if let Some(v) = records.into_iter().next() {
            Ok(Some(v))
        } else {
            Ok(None)
        }
    }

    async fn db_save(&self, db: &DbConn) -> Result<(), Left> {
        let _: Option<SurrealRecord> = db
            .upsert((Self::db_tab_name(), RecordIdKey::from_inner(self.db_id())))
            .content(DbStage::from(self.clone()))
            .await
            .map_err(mls!(ErrCode::DbError))?;
        Ok(())
    }
}
