use surrealdb::sql::Id;

use crate::{
    daemon::{db::DbEntity, DbService},
    typedef::err::Left,
};

use super::User;

impl DbEntity for User {
    fn db_id(&self) -> Id {
        self.raw_id.into()
    }

    fn db_tab_name() -> &'static str {
        "user"
    }
}

impl User {
    #[tracing::instrument(skip(db))]
    pub async fn find_by_username(db: &DbService, username: String) -> Result<Option<Self>, Left> {
        let query = "SELECT * FROM user WHERE username = type::string($username) AND active_status == \"Active\" LIMIT 1;";
        let users: Vec<User> = db.query_manager_bind(query, ("username", username)).await?;
        Ok(users.into_iter().next())
    }
}
