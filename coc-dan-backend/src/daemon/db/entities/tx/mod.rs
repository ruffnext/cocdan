use serde::{Deserialize, Serialize};
use serde_json::json;
use surrealdb::sql::Thing;

use crate::{
    daemon::db::DbConn,
    left_span, mls,
    typedef::err::{ErrCode, Left},
};

#[derive(Serialize, Deserialize, ts_rs::TS)]
#[ts(
    export,
    export_to = "src/daemon/db/entities/tx/tx_aux.d.ts",
    rename = "ITxAux"
)]
pub struct TxAux {
    pub tx_id: i64,

    #[ts(type = "String")]
    pub stage: Thing,

    #[ts(type = "String")]
    pub user: Thing,

    #[ts(type = "String")]
    pub time: surrealdb::sql::Datetime,

    pub action: TxAction,
}

impl TxAux {
    pub async fn new(
        stage: Thing,
        user: Thing,
        action: TxAction,
        db: &DbConn,
    ) -> Result<Self, Left> {
        let create_statement = format!(
            r#"`
            BEGIN TRANSACTION;

            let $max_id = math::max(SELECT VALUE tx_id AS max_id FROM tx WHERE stage = type::record($stage));

            let $max_id = return if $max_id == None {{
                1
            }} else {{
                type::int($max_id) + 1
            }};

            let $id = type::record(string::concat("tx:", type::string($max_id)));

            CREATE $id CONTENT {{
                tx_id: $max_id,
                stage: type::record($stage),
                user: type::record($user),
                time: time::now(),
                action: {action},
            }};

            COMMIT TRANSACTION;
        "#,
            action = serde_json::to_string(&action).unwrap()
        );

        let mut query_res = db
            .query(create_statement)
            .bind(json!({
                "stage": stage.to_string(),
                "user": user.to_string(),
            }))
            .await
            .map_err(mls!(ErrCode::DbError))?;

        let response: Vec<TxAux> = query_res.take(3).map_err(mls!(ErrCode::DbError))?;

        if let Some(tx) = response.into_iter().next() {
            Ok(tx)
        } else {
            Err(left_span!(ErrCode::DbError))
        }
    }
}

#[derive(Serialize, Deserialize, ts_rs::TS)]
#[ts(
    export,
    export_to = "src/daemon/db/entities/tx/tx_action.d.ts",
    rename = "ITxAction"
)]
pub enum TxAction {
    RolePlay(RolePlay),
}

#[derive(Serialize, Deserialize, ts_rs::TS)]
#[ts(
    export,
    export_to = "src/daemon/db/entities/tx/tx_role_play.d.ts",
    rename = "IRolePlay"
)]
pub struct RolePlay {
    #[ts(type = "String")]
    pub avatar: Thing,
    pub text: String,
}
