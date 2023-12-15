use std::collections::HashMap;

use ts_rs::TS;

use crate::entities::avatar;

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default)]
#[ts(export, rename = "IDescriptor", export_to = "bindings/avatar/IDescriptor.ts")]
pub struct Descriptor {
    age : u32,
    career : String,
    gender : String,
    homeland : String,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq)]
#[ts(export, rename = "IMentalStatus", export_to = "bindings/avatar/IMentalStatus.ts")]
pub enum MentalStatus {
    Lucid,
    Fainting,
    TemporaryInsanity,
    IntermittentInsanity,
    PermanentInsanity
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq)]
#[ts(export, rename = "IHealthStatus", export_to = "bindings/avatar/IHealthStatus.ts")]
pub enum HealthStatus {
    Healthy,
    Ill,
    Injured { location : Vec<String> },
    Critical,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq)]
#[ts(export, rename = "IStatus", export_to = "bindings/avatar/IStatus.ts")]
pub struct Status {
    pub hp  : u32,
    pub mp  : u32,
    pub san : u32,
    pub mental_status : MentalStatus,
    pub health_status : HealthStatus,
}

impl Default for Status {
    fn default() -> Self {
        Self { 
            hp: 0, 
            mp: 0, 
            san: 0, 
            mental_status: MentalStatus::Lucid, 
            health_status: HealthStatus::Healthy
        }
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default)]
#[ts(export, rename = "IAttrs", export_to = "bindings/avatar/IAttrs.ts")]
pub struct Attrs {
    pub str : u32,
    pub dex : u32,
    pub pow : u32,
    pub con : u32,
    pub app : u32,
    pub edu : u32,
    pub siz : u32,
    pub int : u32,
    pub mov : u32,
    pub luk : u32,
    pub mov_adj : Option<f32>
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default)]
#[ts(export, rename = "IDetail", export_to = "bindings/avatar/IDetail.ts")]
pub struct Detail {
    pub status : Status,
    pub attrs : Attrs,
    pub descriptor : Descriptor,
    pub skills : HashMap<String, u32>
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq)]
#[ts(export, rename = "IAvatar", export_to = "bindings/IAvatar.ts")]
pub struct IAvatar {
    pub id: i32,
    pub stage_uuid: String,
    pub owner: i32,
    pub name: String,
    pub detail: Detail
}

impl From<avatar::Model> for IAvatar {
    fn from(value: avatar::Model) -> Self {
        Self { 
            id: value.id, 
            stage_uuid: value.stage_uuid, 
            owner: value.owner, 
            name: value.name, 
            detail: serde_json::from_str(value.detail.as_str()).unwrap_or_default()
        }
    }
}