use std::collections::HashMap;

use lazy_static::lazy_static;
use ts_rs::TS;

use super::{avatar::Characteristic, common::EraEnum};

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default, Debug)]
#[ts(export, rename = "ISkill", export_to = "bindings/avatar/ISkill.ts")]
pub struct Skill {
    pub name : String,
    pub initial : u32,
    pub era : EraEnum,
    pub category : SkillCategory
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "ISkillCategory", export_to = "bindings/avatar/ISkillCategory.ts")]
pub enum SkillCategory {
    Any,
    Social,
    ArtAndCraft,
    Fighting,
    Custom
}

impl Default for SkillCategory {
    fn default() -> Self {
        Self::Any
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IOptionalOccupationalSkill", export_to = "bindings/avatar/IOptionalOccupationalSkill.ts")]
pub struct OptionalOccupationalSkill {
    pub category : SkillCategory,
    pub candidates : Vec<String>,       // if candidates is empty, it mean all skill under this category can be selected
    pub limit : u32
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IOccupationalSkill", export_to = "bindings/avatar/IOccupationalSkill.ts")]
#[serde(untagged)]
pub enum OccupationalSkill {
    Identity (String),
    Enumeration (OptionalOccupationalSkill)
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IOccupation", export_to = "bindings/avatar/IOccupation.ts")]
pub struct Occupation {
    pub name : String,
    pub credit_rating : (u32, u32),
    pub era : EraEnum,
    pub characteristics : Vec<Characteristic>,
    pub occupational_skills : Vec<OccupationalSkill>,
}

impl Default for Occupation {
    fn default() -> Self {
        Self {
            name : "Custom".to_string(),
            credit_rating : (0, 100),
            era : EraEnum::None,
            characteristics : vec![Characteristic::Edu],
            occupational_skills : Vec::new(),
        }
    }
}


lazy_static! {
    pub static ref SKILLS : HashMap<String, Skill> = 
        serde_json::from_str::<Vec<Skill>>(include_str!("../../resource/skills.json")).unwrap()
            .into_iter().map(|x| (x.name.to_string(), x)).collect();

    pub static ref OCCUPATIONS : HashMap<String, Occupation> = 
        serde_json::from_str::<Vec<Occupation>>(include_str!("../../resource/occupation.json")).unwrap()
            .into_iter().map(|x| (x.name.to_string(), x)).collect();
}

pub enum SkillAssignType {
    Occupational = 1,
    Optional = 2,
    Interest = 4
}

impl Default for SkillAssignType {
    fn default() -> Self {
        Self::Interest
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default, Debug)]
#[ts(export, rename = "ISkillAssigned", export_to = "bindings/avatar/ISkillAssigned.ts")]
pub struct SkillAssigned {
    pub name : String,
    pub initial : u32,
    pub era : EraEnum,
    pub occupation_skill_point : u32,
    pub interest_skill_point : u32,
    pub category : SkillCategory,
    pub assign_type : u32       // SkillAssignType
}
