use ts_rs::TS;

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug)]
#[ts(export, rename = "IDice", export_to = "bindings/avatar/IDice.ts")]
pub struct Dice (String);

impl Default for Dice {
    fn default() -> Self {
        return Self("".to_string())
    }
}
