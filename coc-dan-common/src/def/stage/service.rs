use ts_rs::TS;

#[derive(serde::Deserialize, serde::Serialize, TS)]
#[ts(export, rename = "ICreateStage", export_to = "bindings/stage/ICreateStage.ts")]
pub struct ICreateStage {
    pub title : String,
    pub description : String
}
