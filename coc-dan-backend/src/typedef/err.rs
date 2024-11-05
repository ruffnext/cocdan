use std::borrow::Cow;

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
    #[error("Invalid Parameter {0}")]
    InvalidParameter(Cow<'static, str>),

    #[error("Database Error")]
    DbError,

    #[error("Internal Server Error")]
    InternalServerError(Cow<'static, str>),

    #[error("Permission Denied {0}")]
    PermissionDenied(Cow<'static, str>),

    #[error("Unsupported Operation {0}")]
    UnsupportedOperation(Cow<'static, str>),

    #[error("Serialize Error")]
    SerdeError,

    #[error("No Content")]
    NoContent,
}

impl ErrCode {
    pub fn to_http_code(&self) -> http::StatusCode {
        match self {
            ErrCode::InvalidParameter(_) => http::StatusCode::BAD_REQUEST,
            ErrCode::DbError => http::StatusCode::INTERNAL_SERVER_ERROR,
            ErrCode::InternalServerError(_) => http::StatusCode::INTERNAL_SERVER_ERROR,
            ErrCode::PermissionDenied(_) => http::StatusCode::FORBIDDEN,
            ErrCode::UnsupportedOperation(_) => http::StatusCode::BAD_REQUEST,
            ErrCode::SerdeError => http::StatusCode::INTERNAL_SERVER_ERROR,
            ErrCode::NoContent => http::StatusCode::NO_CONTENT,
        }
    }
}

/// map left span
#[macro_export]
macro_rules! mls {
    ( $status:expr) => {{
        |e| {
            println!("{:#?}", e);
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
    ( $status:expr ) => {{
        tracing::error!("{} / {}:{} / {}", $status, file!(), line!(), $status);
        crate::typedef::err::Left {
            status: $status.to_http_code(),
            message: $status.to_string(),
            code: None,
        }
    }};
    ( $status:expr, $code:expr ) => {{
        tracing::error!("{} / {}:{} / {}", $status, file!(), line!(), $status);
        crate::typedef::err::Left {
            status: $status.to_http_code(),
            message: $status.to_string(),
            code: Some($code.to_string()),
        }
    }};
}
