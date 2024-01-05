use wasm_bindgen::prelude::wasm_bindgen;

#[derive(Debug, serde::Serialize)]
#[wasm_bindgen]
pub struct Left {
    #[serde(with = "http_status")]
    #[wasm_bindgen(skip)] pub status : http::StatusCode,
    #[wasm_bindgen(skip)] pub message : String,
    #[wasm_bindgen(skip)] pub uuid : &'static str
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

