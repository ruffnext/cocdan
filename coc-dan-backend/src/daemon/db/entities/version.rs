use serde::{Deserialize, Serialize};
use surrealdb::sql::{Datetime, Thing};

#[derive(Debug, Serialize, Deserialize)]
pub struct DbVersion<T> {
    pub content: T,
    pub time: Datetime,
    pub tx: Thing,
}
