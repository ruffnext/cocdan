use chrono::{DateTime, FixedOffset};
use serde::{Deserialize, Serialize};
use surrealdb::sql::Id;

use crate::daemon::db::DbEntity;

use super::User;

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
#[ts(export, rename = "ISession", export_to = "entity/basic/ISession.d.ts")]
pub struct Session {
    #[serde(default)]
    pub raw_id: String,
    #[ts(type = "string")]
    pub login_time: DateTime<FixedOffset>,
    #[ts(type = "string")]
    pub expiration_time: DateTime<FixedOffset>,
    #[ts(inline)]
    pub session_type: SessionType,
}

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
pub enum SessionType {
    User(User),
}

impl DbEntity for Session {
    fn db_id(&self) -> Id {
        self.raw_id.clone().into()
    }

    fn db_tab_name() -> &'static str {
        "session"
    }
}

impl Session {
    pub fn is_expired(&self) -> bool {
        self.expiration_time < chrono::Utc::now().fixed_offset()
    }
}
