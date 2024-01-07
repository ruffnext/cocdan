use ts_rs::TS;

use super::Detail;

#[derive(serde::Deserialize, serde::Serialize, TS)]
#[ts(export, rename = "ICreateAvatar", export_to = "bindings/avatar/service/ICreateAvatar.ts")]
pub struct ICreateAvatar {
    pub name : String,
    pub detail : Option<Detail>,
    pub stage_id : Option<i32>,
}
