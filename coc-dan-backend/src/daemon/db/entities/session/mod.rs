use chrono::{DateTime, FixedOffset};
use serde::{Deserialize, Serialize};
use serde_json::json;
use surrealdb::sql::{Id, Thing};

use crate::{
    daemon::{
        db::{DbConn, DbEntity},
        SurrealRecord,
    },
    mls,
    typedef::err::{ErrCode, Left},
};

use super::User;

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
#[ts(export, rename = "ISession", export_to = "entity/basic/ISession.d.ts")]
pub struct Session {
    #[serde(default)]
    pub raw_id: String,
    #[ts(type = "string")]
    pub login_time: DateTime<FixedOffset>,
    #[ts(type = "string")]
    pub expiration_time: DateTime<FixedOffset>,
    #[ts(inline)]
    pub session_type: SessionType,
}

#[derive(Deserialize, Serialize, Debug, Clone, ts_rs::TS)]
pub enum SessionType {
    User(User),
}

impl DbEntity for Session {
    type IdType = String;

    fn db_id(&self) -> Id {
        self.raw_id.clone().into()
    }

    fn db_tab_name() -> &'static str {
        "session"
    }

    async fn db_load_by_id(id: Self::IdType, db: &DbConn) -> Result<Option<Self>, Left> {
        let query_str = "
            SELECT * FROM session WHERE raw_id == $id LIMIT 1 FETCH session_type.User;
        ";

        let mut response = db
            .query(query_str)
            .bind(("id", id))
            .await
            .map_err(mls!(ErrCode::DbError))?;

        let sessions: Vec<Session> = response.take(0).map_err(mls!(ErrCode::DbError))?;

        return Ok(sessions.into_iter().next());
    }
}

impl Session {
    pub fn is_expired(&self) -> bool {
        self.expiration_time < chrono::Utc::now().fixed_offset()
    }
    pub async fn is_on_stage(&self, stage: String, db: &DbConn) -> bool {
        let user = match self.session_type {
            SessionType::User(ref u) => u,
        };
        let query = "SELECT * FROM r_user_join_stage WHERE in = type::record($user) AND out = type::record($stage) LIMIT 1";
        match db
            .query(query)
            .bind(json!({
                "user": user.db_thing().to_string(),
                "stage": Thing::from(("stage", Id::from(stage))).to_string(),
            }))
            .await
        {
            Ok(mut res) => {
                let v = res.take::<Vec<SurrealRecord>>(0).unwrap_or(vec![]);
                println!("{:?}", v);
                return v.len() > 0;
            }
            Err(_e) => return false,
        };
    }
}
