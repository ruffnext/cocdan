use std::collections::HashMap;

use ts_rs::TS;

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq)]
#[ts(export, rename = "IArea")]
pub struct Area {
    pub id : String,
    pub name : String
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq)]
#[ts(export, rename = "IGameMap")]
pub struct GameMap{
    uuid_to_area : HashMap<String, Area>
}

impl GameMap {
    pub fn new_empty() -> Self {
        Self { uuid_to_area: HashMap::new() }
    }
    pub fn get_area(&self, uuid : &String) -> Option<&Area> {
        self.uuid_to_area.get(uuid)
    }
}
