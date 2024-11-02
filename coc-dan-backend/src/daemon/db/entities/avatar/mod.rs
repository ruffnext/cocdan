use std::collections::HashMap;

use ts_rs::TS;

use super::common::EraEnum;
use super::skill::{OccupationalSkill, SkillAssigned};

use super::weapon::Weapon;
use super::{Stage, User};

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IGender", export_to = "entity/avatar/IGender.d.ts")]
pub enum Gender {
    Other,
    Male,
    Female,
}

impl Default for Gender {
    fn default() -> Self {
        Self::Other
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default, Debug, Clone)]
#[ts(
    export,
    rename = "IDescriptor",
    export_to = "entity/avatar/IDescriptor.d.ts"
)]
pub struct Descriptor {
    age: u32,
    gender: Gender,
    homeland: String,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IMentalStatus",
    export_to = "entity/avatar/IMentalStatus.d.ts"
)]
pub enum MentalStatus {
    Lucid,
    Fainting,
    TemporaryInsanity,
    IndefiniteInsanity,
    PermanentInsanity,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IHealthStatus",
    export_to = "entity/avatar/IHealthStatus.d.ts"
)]
pub enum HealthStatus {
    Healthy,
    Ill,
    Injured,
    Critical,
    Dead,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IStatus", export_to = "entity/avatar/IStatus.d.ts")]
pub struct Status {
    pub hp: u32,
    pub mp: u32,
    pub san: u32,
    pub hp_loss: u32,
    pub mp_loss: u32,
    pub san_loss: u32,
    pub mental_status: MentalStatus,
    pub health_status: HealthStatus,
}

impl Default for Status {
    fn default() -> Self {
        Self {
            hp: 0,
            mp: 0,
            san: 0,
            hp_loss: 0,
            mp_loss: 0,
            san_loss: 0,
            mental_status: MentalStatus::Lucid,
            health_status: HealthStatus::Healthy,
        }
    }
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Default, Debug, Clone)]
#[ts(
    export,
    rename = "ICharacteristics",
    export_to = "entity/avatar/ICharacteristics.d.ts"
)]
pub struct Characteristics {
    pub str: u32,
    pub dex: u32,
    pub pow: u32,
    pub con: u32,
    pub app: u32,
    pub edu: u32,
    pub siz: u32,
    pub int: u32,
    pub mov: u32,
    pub luk: u32,
    pub mov_adj: Option<f32>,
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "ICharacteristicEnum",
    export_to = "entity/avatar/ICharacteristicEnum.d.ts"
)]
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
    Luk,
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "ICustomEquipment",
    export_to = "entity/avatar/ICustomEquipment.d.ts"
)]
pub struct CustomEquipment {
    pub description: String,
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IEquipmentItem",
    export_to = "entity/avatar/IEquipmentItem.d.ts"
)]
pub enum EquipmentItem {
    Weapon(Weapon),
    Custom(CustomEquipment),
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IEquipment",
    export_to = "entity/avatar/IEquipment.d.ts"
)]
pub struct Equipment {
    pub name: String,
    pub item: EquipmentItem,
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IDetail", export_to = "entity/avatar/IDetail.d.ts")]
pub struct Detail {
    pub status: Status,
    pub characteristics: Characteristics,
    pub descriptor: Descriptor,
    pub skills: HashMap<String, SkillAssigned>,
    pub occupation: Occupation,
    pub equipments: Vec<Equipment>,
}

impl Default for Detail {
    fn default() -> Self {
        Self {
            status: Default::default(),
            characteristics: Default::default(),
            descriptor: Default::default(),
            skills: Default::default(),
            occupation: Default::default(),
            equipments: Default::default(),
        }
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IOccupation",
    export_to = "entity/avatar/IOccupation.d.ts"
)]
pub struct Occupation {
    pub name: String,
    pub credit_rating: (u32, u32),
    pub era: EraEnum,
    pub characteristics: Vec<Characteristic>,
    pub occupational_skills: Vec<OccupationalSkill>,
}

impl Default for Occupation {
    fn default() -> Self {
        Self {
            name: "Custom".to_string(),
            credit_rating: (0, 100),
            era: EraEnum::None,
            characteristics: vec![Characteristic::Edu],
            occupational_skills: Vec::new(),
        }
    }
}

#[derive(serde::Deserialize, serde::Serialize, TS, Debug, Clone)]
#[ts(export, rename = "IAvatar", export_to = "entity/avatar/IAvatar.d.ts")]
pub struct IAvatar {
    pub raw_id: String,
    pub of_stage: Stage,
    pub owner: User,
    pub name: String,
    pub detail: Detail,
    pub header: Option<String>,
}
