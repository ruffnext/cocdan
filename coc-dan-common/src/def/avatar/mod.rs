pub mod service;
use std::collections::HashMap;

use ts_rs::TS;

use super::skills::{Occupation, OCCUPATIONS, SkillAssigned};

use super::weapon::Weapon;

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IGender", export_to = "bindings/avatar/IGender.ts")]
pub enum Gender {
    Other,
    Male,
    Female
}

impl Default for Gender {
    fn default() -> Self {
        Self::Other
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default, Debug, Clone)]
#[ts(export, rename = "IDescriptor", export_to = "bindings/avatar/IDescriptor.ts")]
pub struct Descriptor {
    age : u32,
    gender : Gender,
    homeland : String,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IMentalStatus", export_to = "bindings/avatar/IMentalStatus.ts")]
pub enum MentalStatus {
    Lucid,
    Fainting,
    TemporaryInsanity,
    IndefiniteInsanity,
    PermanentInsanity
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IHealthStatus", export_to = "bindings/avatar/IHealthStatus.ts")]
pub enum HealthStatus {
    Healthy,
    Ill,
    Injured,
    Critical,
    Dead
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IStatus", export_to = "bindings/avatar/IStatus.ts")]
pub struct Status {
    pub hp  : u32,
    pub mp  : u32,
    pub san : u32,
    pub hp_loss : u32,
    pub mp_loss : u32,
    pub san_loss : u32,
    pub mental_status : MentalStatus,
    pub health_status : HealthStatus,
}

impl Default for Status {
    fn default() -> Self {
        Self { 
            hp: 0, 
            mp: 0, 
            san: 0, 
            hp_loss : 0,
            mp_loss : 0,
            san_loss : 0,
            mental_status: MentalStatus::Lucid, 
            health_status: HealthStatus::Healthy
        }
    }
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Default, Debug, Clone)]
#[ts(export, rename = "ICharacteristics", export_to = "bindings/avatar/ICharacteristics.ts")]
pub struct Characteristics {
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

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "ICharacteristicEnum", export_to = "bindings/avatar/ICharacteristicEnum.ts")]
#[serde(rename_all = "lowercase")]
pub enum Characteristic {
    Str,
    Dex,
    Pow,
    Con,
    App,
    Edu,
    Siz,
    Int,
    Mov,
    Luk
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "ICustomEquipment", export_to = "bindings/ICustomEquipment.ts")]
pub struct CustomEquipment {
    pub description : String
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IEquipmentItem", export_to = "bindings/IEquipmentItem.ts")]
pub enum EquipmentItem {
    Weapon(Weapon),
    Custom(CustomEquipment)
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IEquipment", export_to = "bindings/IEquipment.ts")]
pub struct Equipment {
    pub name : String,
    pub item : EquipmentItem
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IDetail", export_to = "bindings/avatar/IDetail.ts")]
pub struct Detail {
    pub status : Status,
    pub characteristics : Characteristics,
    pub descriptor : Descriptor,
    pub skills : HashMap<String, SkillAssigned>,
    pub occupation : Occupation,
    pub equipments : Vec<Equipment>
}

impl Default for Detail {
    fn default() -> Self {
        let occupation: Occupation = match OCCUPATIONS.get("Accountant") {
            Some(v) => v.clone(),
            None => Occupation::default()
        };
        Self { 
            status: Default::default(), 
            characteristics: Default::default(), 
            descriptor: Default::default(), 
            skills: Default::default(), 
            occupation,
            equipments: Default::default()
        }
    }
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IAvatar", export_to = "bindings/IAvatar.ts")]
pub struct IAvatar {
    pub id: i32,
    pub stage_uuid: Option<String>,
    pub owner: i32,
    pub name: String,
    pub detail: Detail
}
