use serde::{Deserialize, Serialize};
use surrealdb::{
    sql::Datetime,
    sql::{Id, Thing},
};

use crate::{
    daemon::{db::DbConn, DbEntity},
    left_span, mls,
    typedef::err::{ErrCode, Left},
};

use super::AvatarAux;

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct TxAux {
    pub id: Thing,

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
        stage: Thing,
        user: Thing,
        avatar: Thing,
        action: TxAction,
        db: &DbConn,
    ) -> Result<Self, Left> {
        let raw_id = rand::random::<i64>();
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

            CREATE $id SET
                tx_id = $max_id,
                raw_id = {raw_id},
                stage = $stage,
                user = $user,
                avatar = type::record({avatar}),
                time = time::now(),
                action = None;

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
            .await
            .map_err(mls!(ErrCode::DbError))?;

        #[derive(Deserialize)]
        struct TxHelper {
            id: Thing,
            tx_id: i64,
            time: Datetime,
        }

        let response: Vec<TxHelper> = query_res.take(5).map_err(mls!(ErrCode::DbError))?;

        if let Some(tx) = response.into_iter().next() {
            // for trigger live select
            let tx_aux = TxAux {
                id: tx.id,
                tx_id: tx.tx_id,
                raw_id,
                stage,
                user,
                avatar,
                time: tx.time,
                action,
            };
            let res: Option<TxAux> = db
                .upsert((TxAux::db_tab_name(), raw_id))
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
    AvatarAdd(AvatarAux),
    AvatarDel(AvatarAux),
    /// before, after
    AvatarModify(AvatarAux, AvatarAux),
}

#[derive(Serialize, Deserialize, Clone, ts_rs::TS, Debug)]
#[ts(export, export_to = "entity/tx/IRolePlay.d.ts", rename = "IRolePlay")]
pub struct RolePlay {
    pub text: String,
}

#[derive(Serialize, ts_rs::TS, Clone)]
#[ts(export, export_to = "api/ws/tx/ITxEvent.d.ts", rename = "ITxEvent")]
pub struct TxEvent {
    pub tx_id: i64,
    pub raw_id: i64,
    pub stage_id: i64,
    pub user_id: i64,
    pub avatar_id: String,
    pub time: String,
    pub action: TxAction,
}
