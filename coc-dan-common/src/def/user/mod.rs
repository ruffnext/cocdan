pub mod service;

use ts_rs::TS;

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq)]
#[ts(export)]
pub struct IUser {
    pub id : i32,
    pub name : String,
    pub nick_name : String,
    pub header : String
}
