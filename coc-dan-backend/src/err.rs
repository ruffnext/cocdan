use axum::{response::IntoResponse, Json};
use sea_orm::{DbErr, TransactionError};

#[derive(Debug, serde::Serialize)]
pub struct Left {
    #[serde(skip)]
    pub status : http::StatusCode,
    pub message : String,
    pub uuid : &'static str
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

impl IntoResponse for Left {
    fn into_response(self) -> axum::response::Response {
        if self.status == http::StatusCode::NO_CONTENT {
            self.status.into_response()
        } else {
            (self.status, Json(self)).into_response()
        }
    }
}
