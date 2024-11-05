use serde::{Deserialize, Serialize};
use surrealdb::sql::{Id, Thing};

use crate::{
    daemon::{db::DbConn, DbEntity},
    left_span, mls,
    typedef::err::{ErrCode, Left},
};

use super::AvatarAux;

#[derive(Serialize, Deserialize, Clone)]
pub struct TxAux {
    pub tx_id: i64,

    pub raw_id: i64,

    pub stage: Thing,

    pub user: Thing,

    pub avatar: Thing,

    pub time: surrealdb::sql::Datetime,

    pub action: TxAction,
}

impl DbEntity for TxAux {
    type IdType = String;
    fn db_tab_name() -> &'static str {
        "tx"
    }
    fn db_id(&self) -> surrealdb::sql::Id {
        Id::from(self.raw_id.clone())
    }
}

impl TxAux {
    pub async fn new(
        raw_id: i64,
        stage: Thing,
        user: Thing,
        avatar: Thing,
        action: TxAction,
        db: &DbConn,
    ) -> Result<Self, Left> {
        let create_statement = format!(
            r#"
            BEGIN TRANSACTION;

            let $stage = type::record({stage});

            let $user = type::record({user});

            let $max_id = math::max(SELECT VALUE tx_id AS max_id FROM tx WHERE stage = $stage);

            let $max_id = return if $max_id == None {{
                1
            }} else {{
                type::int($max_id) + 1
            }};

            let $id = type::record({id});

            CREATE $id CONTENT {{
                tx_id: $max_id,
                raw_id: {raw_id},
                stage: $stage,
                user: $user,
                avatar: type::record({avatar}),
                time: time::now(),
                action: $action,
            }};

            COMMIT TRANSACTION;
        "#,
            stage = stage.to_string(),
            user = user.to_string(),
            avatar = avatar.to_string(),
            raw_id = raw_id,
            id = Thing::from((TxAux::db_tab_name(), Id::from(raw_id))).to_string(),
        );

        let mut query_res = db
            .query(create_statement)
            .bind(("action", action))
            .await
            .map_err(mls!(ErrCode::DbError))?;

        let response: Vec<TxAux> = query_res.take(5).map_err(mls!(ErrCode::DbError))?;

        if let Some(tx) = response.into_iter().next() {
            Ok(tx)
        } else {
            Err(left_span!(ErrCode::DbError))
        }
    }
}

#[derive(Serialize, Deserialize, Clone, ts_rs::TS)]
#[ts(export, export_to = "entity/tx/ITxAction.d.ts", rename = "ITxAction")]
pub enum TxAction {
    RolePlay(RolePlay),
    AvatarAdd(AvatarAux),
    AvatarDel(AvatarAux),
    /// before, after
    AvatarModify(AvatarAux, AvatarAux),
}

#[derive(Serialize, Deserialize, Clone, ts_rs::TS)]
#[ts(export, export_to = "entity/tx/IRolePlay.d.ts", rename = "IRolePlay")]
pub struct RolePlay {
    pub text: String,
}
