use chrono::{DateTime, FixedOffset};
use serde::{Deserialize, Serialize};
use surrealdb::sql::Id;

use super::super::{DbEntity, DbService};
use crate::typedef::err::Left;

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
#[ts(export, rename = "IUser", export_to = "entity/basic/IUser.d.ts")]
pub struct User {
    pub raw_id: String,
    pub username: String,
    pub nickname: String,
    #[ts(inline)]
    pub active_status: UserActiveStatus,
    #[ts(type = "string")]
    pub registration_time: DateTime<FixedOffset>,
}

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
pub enum UserActiveStatus {
    Active,
    Banned,
}

impl DbEntity for User {
    type IdType = String;

    fn db_id(&self) -> Id {
        Id::from(self.raw_id.clone())
    }

    fn db_tab_name() -> &'static str {
        "user"
    }
}

impl User {
    #[tracing::instrument(skip(db))]
    pub async fn find_by_username(db: &DbService, username: String) -> Result<Option<Self>, Left> {
        let query = "SELECT * FROM user WHERE username = type::string($username) AND active_status == \"Active\" LIMIT 1;";
        let users: Vec<User> = db.query_manager_bind(query, ("username", username)).await?;
        Ok(users.into_iter().next())
    }
}
