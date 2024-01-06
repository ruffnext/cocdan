use ts_rs::TS;

#[derive(serde::Deserialize, serde::Serialize, TS)]
#[ts(export, rename = "IQueryStageTxs", export_to = "bindings/tx/service/IQueryStageTxs.ts")]
pub struct IQueryStageTxs {
    pub begin : Option<u32>,
    pub end   : Option<u32>
}