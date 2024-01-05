use sea_orm::{DbErr, TransactionError};
use wasm_bindgen::prelude::wasm_bindgen;

#[derive(Debug, serde::Serialize)]
#[wasm_bindgen]
pub struct Left {
    #[serde(with = "http_status")]
    #[wasm_bindgen(skip)] pub status : http::StatusCode,
    #[wasm_bindgen(skip)] pub message : String,
    #[wasm_bindgen(skip)] pub uuid : &'static str
}

impl From<DbErr> for Left {
    fn from(value: DbErr) -> Self {
        Self { 
            status: http::StatusCode::INTERNAL_SERVER_ERROR, 
            message: format!("Database Error : ({value})"),
            uuid: "a0bcfe69"
        }
    }
}

impl From<TransactionError<DbErr>> for Left {
    fn from(value: TransactionError<DbErr>) -> Self {
        Self { 
            status: http::StatusCode::INTERNAL_SERVER_ERROR, 
            message: format!("Transaction Error : ({value})"), 
            uuid: "9919ccce" 
        }
    }
}


mod http_status {
    use serde::Serializer;

    pub fn serialize<S>(
        data: &http::StatusCode,
        serializer: S,
    ) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_u16(data.as_u16())
    }
}
