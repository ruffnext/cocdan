use coc_dan_common::def::transaction::ITransaction;

use crate::entities::transaction::Model;

impl From<Model> for ITransaction {
    fn from(value: Model) -> Self {
        Self {
            id : value.id,
            tx_id : value.tx_id as usize,
            stage_uuid : value.stage_uuid,
            user_id : value.user_id,
            avatar_id : value.avatar_id,
            time : value.time,
            tx : serde_json::from_str(&value.tx).unwrap()
        }
    }
}
