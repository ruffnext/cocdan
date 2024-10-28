use surrealdb::sql::Id;

use crate::daemon::db::DbEntity;

use super::Session;

impl DbEntity for Session {
    fn db_id(&self) -> Id {
        self.raw_id.clone().into()
    }

    fn db_tab_name() -> &'static str {
        "session"
    }
}
