pub mod service;
use ts_rs::TS;

use super::avatar::IAvatar;

#[derive(serde::Deserialize, serde::Serialize, Debug, Clone, PartialEq, TS)]
#[ts(export, rename = "ITransaction", export_to = "bindings/tx/ITransaction.ts")]
pub struct ITransaction {
    pub tx_id : usize,
    pub stage_uuid : String,
    pub user_id : i32,
    pub avatar_id : i32,
    pub time : String,
    pub tx : Tx
}

#[derive(serde::Deserialize, serde::Serialize, Debug, Clone, PartialEq, TS)]
#[ts(export, rename = "ITx", export_to = "bindings/tx/ITx.ts")]
pub enum Tx {
    Statement (String),
    Dice (Dice),
    UpdateAvatar {
        before : Option<IAvatar>,
        after : Option<IAvatar>
    }
}

#[derive(serde::Deserialize, serde::Serialize, Debug, Clone, PartialEq, TS)]
#[ts(export, rename = "IDice", export_to = "bindings/dice/IDice.ts")]
pub struct Dice {
    pub tx_id : u32,
    pub result : DiceResult,
    pub command : String,       // sc 1/1d3, rc str, rb LibraryUse....
}

#[derive(serde::Deserialize, serde::Serialize, Debug, Clone, PartialEq, TS)]
#[ts(export, rename = "IDiceResult", export_to = "bindings/dice/IDiceResult.ts")]
pub enum DiceResult {
    DicePending,
    DiceNaive (DiceNaive),
    DiceCheck (DiceCheck),
    DiceSanCheck (DiceSanCheck)
}

#[derive(serde::Deserialize, serde::Serialize, Debug, Clone, PartialEq, TS)]
#[ts(export, rename = "IDiceNaive", export_to = "bindings/dice/IDiceNaive.ts")]
pub struct DiceNaive {  // naive dice, r1d3, r2d10, r1d100 * 2......
    pub result : u32
}

#[derive(serde::Deserialize, serde::Serialize, Debug, Clone, PartialEq, TS)]
#[ts(export, rename = "IDiceCheck", export_to = "bindings/dice/IDiceCheck.ts")]
pub struct DiceCheck {  // check dice, rc str, rc pow, rc str/2
    pub result : u32,
    pub rate_of_success : u32,
    pub dice_against : String   // what you are dicing against
}

#[derive(serde::Deserialize, serde::Serialize, Debug, Clone, PartialEq, TS)]
#[ts(export, rename = "IDiceSanLoss", export_to = "bindings/dice/IDiceSanLoss.ts")]
pub enum DiceSanLoss {
    Success (u32),
    Failure (u32)
}

#[derive(serde::Deserialize, serde::Serialize, Debug, Clone, PartialEq, TS)]
#[ts(export, rename = "IDiceSanCheck", export_to = "bindings/dice/IDiceSanCheck.ts")]
pub struct DiceSanCheck {   // sc 1/1d3
    pub san_check : u32,    // san check dice rolled out
    pub san_loss : DiceSanLoss
}
