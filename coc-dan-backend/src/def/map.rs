use std::collections::HashMap;

use uuid::Uuid;

#[derive(serde::Serialize)]
pub struct Area {
    pub id : Uuid,
    pub name : String
}

#[derive(serde::Serialize)]
pub struct GameMap {
    uuid_to_area : HashMap<Uuid, Area>
}

impl GameMap {
    pub fn new_empty() -> Self {
        Self { uuid_to_area: HashMap::new() }
    }
    pub fn get_area(&self, uuid : &Uuid) -> Option<&Area> {
        self.uuid_to_area.get(&uuid)
    }
}
