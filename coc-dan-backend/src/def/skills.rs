use std::collections::HashMap;

use lazy_static::lazy_static;
use ts_rs::TS;

use super::avatar::Attribute;

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IEraEnum", export_to = "bindings/IEraEnum.ts")]
pub enum EraEnum {
    None,
    Modern,
    Contemporary
}

impl Default for EraEnum {
    fn default() -> Self {
        Self::None
    }
}

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
    Social
}

impl Default for SkillCategory {
    fn default() -> Self {
        Self::Any
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IOccupation", export_to = "bindings/avatar/IOccupation.ts")]
pub struct Occupation {
    pub name : String,
    pub credit_rating : (u32, u32),
    pub era : EraEnum,
    pub attribute : Vec<Attribute>,
    pub occupational_skills : Vec<String>,
    pub additional_skills : Vec<SkillCategory>
}

impl Default for Occupation {
    fn default() -> Self {
        Self {
            name : "Custom".to_string(),
            credit_rating : (0, 100),
            era : EraEnum::None,
            attribute : vec![Attribute::Edu],
            occupational_skills : Vec::new(),
            additional_skills : vec![]
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

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug)]
#[ts(export, rename = "ISkillAssignType", export_to = "bindings/avatar/ISkillAssignType.ts")]
pub enum SkillAssignType {
    Occupational,
    AdditionalOccupational,
    Interest
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
    pub assign_type : SkillAssignType
}
