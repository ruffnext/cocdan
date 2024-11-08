use serde::{Deserialize, Serialize};
use surrealdb::{
    sql::{Datetime, Id, Thing},
    RecordIdKey,
};
use tracing::info;

use crate::{
    daemon::{db::DbConn, entities::Stage, DbEntity},
    left_span, mls,
    typedef::err::{ErrCode, Left},
};

use super::AvatarDetail;

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct TxAux {
    pub id: Thing,

    pub raw_id: String,

    pub tx_index: i64,

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
        stage: String,
        user: String,
        avatar: String,
        action: TxAction,
        db: &DbConn,
    ) -> Result<Self, Left> {
        let create_statement = format!(
            r#"
            BEGIN TRANSACTION;

            let $stage = type::record(stage:`{stage}`);

            let $user = type::record(user:`{user}`);

            let $max_id = math::max(SELECT VALUE tx_index AS max_id FROM tx WHERE stage = $stage);

            let $max_id = return if $max_id == None {{
                1
            }} else {{
                type::int($max_id) + 1
            }};

            let $raw_id = string::concat('{stage}-',  $max_id);

            CREATE type::record(string::concat('tx:', '`', $raw_id, '`')) SET
                raw_id = $raw_id,
                tx_index = $max_id,
                stage = $stage,
                user = $user,
                avatar = type::record(string::concat('avatar:', '`', '{avatar}', '`')),
                time = time::now(),
                action = None;

            COMMIT TRANSACTION;
        "#
        );

        info!("{}", create_statement);

        let mut query_res = db
            .query(create_statement)
            .await
            .map_err(mls!(ErrCode::DbError))?;

        #[derive(Deserialize)]
        struct TxHelper {
            id: Thing,
            tx_index: i64,
            raw_id: String,
            time: Datetime,
        }

        let response: Vec<TxHelper> = query_res.take(5).map_err(mls!(ErrCode::DbError))?;

        if let Some(tx) = response.into_iter().next() {
            // for trigger live select
            let tx_aux = TxAux {
                id: tx.id,
                tx_index: tx.tx_index,
                raw_id: tx.raw_id,
                stage: Thing::from((Stage::db_tab_name(), Id::from(stage))),
                user: Thing::from(("user", Id::from(user))),
                avatar: Thing::from(("avatar", Id::from(avatar))),
                time: tx.time,
                action,
            };
            let res: Option<TxAux> = db
                .upsert((
                    TxAux::db_tab_name(),
                    RecordIdKey::from_inner(tx_aux.db_id()),
                ))
                .content(tx_aux)
                .await
                .map_err(mls!(ErrCode::DbError))?;
            if let Some(v) = res {
                Ok(v)
            } else {
                Err(left_span!(ErrCode::InternalServerError(
                    "Failed to create tx".into()
                )))
            }
        } else {
            Err(left_span!(ErrCode::DbError))
        }
    }
}

#[derive(Serialize, Deserialize, Clone, ts_rs::TS, Debug)]
#[ts(export, export_to = "entity/tx/ITxAction.d.ts", rename = "ITxAction")]
pub enum TxAction {
    RolePlay(RolePlay),
    AvatarAdd((String, AvatarDetail)),
    AvatarDel((String, AvatarDetail)),
    /// id before, after
    AvatarModify((String, AvatarDetail, AvatarDetail)),
}

#[derive(Serialize, Deserialize, Clone, ts_rs::TS, Debug)]
#[ts(export, export_to = "entity/tx/IRolePlay.d.ts", rename = "IRolePlay")]
pub struct RolePlay {
    pub text: String,
}

#[derive(Serialize, Deserialize, ts_rs::TS, Clone)]
#[ts(export, export_to = "api/ws/tx/ITxEvent.d.ts", rename = "ITxEvent")]
pub struct TxEvent {
    pub raw_id: String,
    pub tx_index: i64,
    pub stage_id: String,
    pub user_id: String,
    pub avatar_id: String,
    pub time: String,
    pub action: TxAction,
}
