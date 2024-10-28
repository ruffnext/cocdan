use axum::{response::IntoResponse, Json};
use serde::{Deserialize, Serialize};
use serde_json::json;

#[derive(Debug, serde::Serialize)]
pub struct Left {
    #[serde(with = "http_status")]
    pub status: http::StatusCode,
    pub message: String,
    pub code: Option<String>,
}

// impl From<DbErr> for Left {
//     fn from(value: DbErr) -> Self {
//         Self {
//             status: http::StatusCode::INTERNAL_SERVER_ERROR,
//             message: format!("Database Error : ({value})"),
//             uuid: "a0bcfe69",
//         }
//     }
// }

mod http_status {
    use serde::Serializer;

    pub fn serialize<S>(data: &http::StatusCode, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_u16(data.as_u16())
    }
}

impl IntoResponse for Left {
    fn into_response(self) -> axum::response::Response {
        (self.status, Json(json!(self))).into_response()
    }
}

#[derive(Debug, Serialize, Deserialize, thiserror::Error)]
pub enum ErrCode {
    #[error("Invalid Parameter")]
    InvalidParameter,

    #[error("Database Error")]
    DbError,

    #[error("Internal Server Error")]
    InternalServerError(String),
}

impl ErrCode {
    pub fn to_http_code(&self) -> http::StatusCode {
        match self {
            ErrCode::InvalidParameter => http::StatusCode::BAD_REQUEST,
            ErrCode::DbError => http::StatusCode::INTERNAL_SERVER_ERROR,
            ErrCode::InternalServerError(_) => http::StatusCode::INTERNAL_SERVER_ERROR,
        }
    }
}

/// map left span
#[macro_export]
macro_rules! mls {
    ( $status:expr) => {{
        |e| {
            tracing::error!("{} {}:{} {e:#?}", $status, file!(), line!());
            crate::typedef::err::Left {
                status: $status.to_http_code(),
                message: format!("{:#?}", $status),
                code: None,
            }
        }
    }};
    ( $status:expr, $code:expr) => {{
        |e| {
            tracing::error!("{} {}:{} {e:#?}", $status, file!(), line!());
            crate::typedef::err::Left {
                status: $status.to_http_code(),
                message: format!("{:#?}", $status),
                code: Some($code.to_string()),
            }
        }
    }};
}

#[macro_export]
macro_rules! left_span {
    ( $status:expr, $message:expr ) => {{
        tracing::error!("{} {}:{} {}", $status, file!(), line!(), $message);
        crate::typedef::err::Left {
            status: $status,
            message: $message.to_string(),
            code: None,
        }
    }};
    ( $status:expr, $message:expr, $code:expr ) => {{
        tracing::error!("{} {}:{} {}", $status, file!(), line!(), $message);
        crate::typedef::err::Left {
            status: $status,
            message: $message.to_string(),
            code: Some($code.to_string()),
        }
    }};
}
