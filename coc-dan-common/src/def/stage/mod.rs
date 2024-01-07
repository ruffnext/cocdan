pub mod service;

use ts_rs::TS;

use super::GameMap;

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IStage", export_to = "bindings/IStage.ts")]
pub struct IStage {
    pub id : i32,
    pub owner : i32,
    pub title : String,
    pub description : String,
    pub game_map : GameMap 
}
