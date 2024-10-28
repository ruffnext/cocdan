use chrono::{DateTime, FixedOffset};
use serde::{Deserialize, Serialize};

mod session;
mod user;

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
#[ts(export, rename = "IUser", export_to = "api/basic/user.d.ts")]
pub struct User {
    pub raw_id: i64,
    pub username: String,
    pub nickname: String,
    pub active_status: UserActiveStatus,
    #[ts(type = "string")]
    pub registration_time: DateTime<FixedOffset>,
}

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
#[ts(export, rename = "ISession", export_to = "api/basic/session.d.ts")]
pub struct Session {
    #[serde(default)]
    pub raw_id: String,
    #[ts(type = "string")]
    pub login_time: DateTime<FixedOffset>,
    pub session_type: SessionType,
}

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
#[ts(export, rename = "ISessionType", export_to = "api/basic/session_type.d.ts")]
pub enum SessionType {
    User(User),
}

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
#[ts(
    export,
    rename = "IUserActiveStatus",
    export_to = "api/basic/user_active_status.d.ts"
)]
pub enum UserActiveStatus {
    Active,
    Baned,
}
