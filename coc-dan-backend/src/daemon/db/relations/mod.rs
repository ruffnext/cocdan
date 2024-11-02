use std::hash::{DefaultHasher, Hash, Hasher};

use serde::Serialize;
use surrealdb::sql::{Id, Thing};

use super::{
    entities::{Stage, User},
    DbEntity, DbRelation,
};

#[derive(Clone, Serialize)]
pub struct RelUserToStage {
    #[serde(rename = "in")]
    inlet: Thing,

    #[serde(rename = "out")]
    outlet: Thing,
}

impl DbRelation<User, Stage, ()> for RelUserToStage {
    fn rel_id(&self) -> Id {
        let mut hasher = DefaultHasher::new();
        self.inlet.hash(&mut hasher);
        self.outlet.hash(&mut hasher);
        let raw_id = format!("{:x}", hasher.finish());
        Id::from(raw_id)
    }

    fn rel_new(f: &User, t: &Stage, _p: &()) -> Self {
        Self {
            inlet: f.db_thing(),
            outlet: t.db_thing(),
        }
    }

    fn rel_table() -> &'static str {
        "r_user_join_stage"
    }
}
