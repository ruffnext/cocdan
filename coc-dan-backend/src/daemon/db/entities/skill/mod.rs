use ts_rs::TS;

use super::common::EraEnum;

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default, Debug)]
#[ts(export, rename = "ISkill", export_to = "entity/skill/ISkill.d.ts")]
pub struct Skill {
    pub name: String,
    pub initial: u32,
    pub era: EraEnum,
    pub category: SkillCategory,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "ISkillCategory",
    export_to = "entity/skill/ISkillCategory.d.ts"
)]
pub enum SkillCategory {
    Any,
    Social,
    ArtAndCraft,
    Fighting,
    Custom,
}

impl Default for SkillCategory {
    fn default() -> Self {
        Self::Any
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IOptionalOccupationalSkill",
    export_to = "entity/skill/IOptionalOccupationalSkill.d.ts"
)]
pub struct OptionalOccupationalSkill {
    pub category: SkillCategory,
    pub candidates: Vec<String>, // if candidates is empty, it mean all skill under this category can be selected
    pub limit: u32,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IOccupationalSkill",
    export_to = "entity/skill/IOccupationalSkill.ts"
)]
#[serde(untagged)]
pub enum OccupationalSkill {
    Identity(String),
    Enumeration(OptionalOccupationalSkill),
}

#[allow(unused)]
pub enum SkillAssignType {
    Occupational = 1,
    Optional = 2,
    Interest = 4,
}

impl Default for SkillAssignType {
    fn default() -> Self {
        Self::Interest
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default, Debug, Clone)]
#[ts(
    export,
    rename = "ISkillAssigned",
    export_to = "entity/avatar/ISkillAssigned.d.ts"
)]
pub struct SkillAssigned {
    pub name: String,
    pub initial: u32,
    pub era: EraEnum,
    pub occupation_skill_point: u32,
    pub interest_skill_point: u32,
    pub category: SkillCategory,
    pub assign_type: u32, // SkillAssignType
}
