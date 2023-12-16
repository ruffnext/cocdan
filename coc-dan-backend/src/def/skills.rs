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
    pub initial : i32,
    pub era : EraEnum
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IOccupation", export_to = "bindings/avatar/IOccupation.ts")]
pub struct Occupation {
    pub name : String,
    pub credit_rating : (i32, i32),
    pub era : EraEnum,
    pub attribute : Vec<Attribute>,
    pub occupational_skills : Vec<String>,
    pub additional_skill_num : i32
}

impl Default for Occupation {
    fn default() -> Self {
        Self {
            name : "Custom".to_string(),
            credit_rating : (0, 100),
            era : EraEnum::None,
            attribute : vec![Attribute::Edu],
            occupational_skills : Vec::new(),
            additional_skill_num : 2
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
