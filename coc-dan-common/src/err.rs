use ts_rs::TS;
use wasm_bindgen::prelude::wasm_bindgen;

#[derive(Debug, serde::Serialize, TS)]
#[ts(export, rename = "ILeft", export_to = "bindings/ILeft.ts")]
#[wasm_bindgen]
pub struct Left {
    #[wasm_bindgen(skip)] pub status : u16,
    #[wasm_bindgen(skip)] pub message : String,
    #[wasm_bindgen(skip)] pub uuid : &'static str
}
