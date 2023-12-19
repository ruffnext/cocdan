use ts_rs::TS;

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IEraEnum", export_to = "bindings/IEraEnum.ts")]
pub enum EraEnum {
    None,               //  Equivalent to "Any"
    Modern,             //  19th to 20th century, including 1920s
    Contemporary        //  21st century to present
}

impl Default for EraEnum {
    fn default() -> Self {
        Self::None
    }
}
