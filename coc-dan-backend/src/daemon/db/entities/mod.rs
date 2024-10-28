use chrono::{DateTime, FixedOffset};
use serde::{Deserialize, Serialize};

mod session;
mod user;

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
#[ts(export, rename = "IUser", export_to = "entity/basic/user.d.ts")]
pub struct User {
    pub raw_id: i64,
    pub username: String,
    pub nickname: String,
    #[ts(inline)]
    pub active_status: UserActiveStatus,
    #[ts(type = "string")]
    pub registration_time: DateTime<FixedOffset>,
}

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
#[ts(export, rename = "ISession", export_to = "entity/basic/session.d.ts")]
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

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
pub enum UserActiveStatus {
    Active,
    Banned,
}

impl Session {
    pub fn is_expired(&self) -> bool {
        self.expiration_time < chrono::Utc::now().fixed_offset()
    }
}
