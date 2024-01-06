use std::collections::HashMap;
use wasm_bindgen::{prelude::wasm_bindgen, JsValue};

use crate::err::Left;

use super::{GameMap, transaction::{ITransaction, Tx}, avatar::IAvatar};


#[derive(serde::Serialize, serde::Deserialize, PartialEq, Default, Debug, Clone)]
#[wasm_bindgen]
pub struct StateMachine {
    #[wasm_bindgen(skip)] pub idx : usize,        // current maximum transaction id, and also total size of txs
    #[wasm_bindgen(skip)] pub idx_base : usize,   // idx_base is included, minimum is 1
    #[wasm_bindgen(skip)] pub txs : Vec<ITransaction>,
    #[wasm_bindgen(skip)] pub state : State
}

#[derive(serde::Serialize, serde::Deserialize, PartialEq, Default, Debug, Clone)]
#[wasm_bindgen]
pub struct State {
    #[wasm_bindgen(skip)] pub idx : usize,
    #[wasm_bindgen(skip)] pub avatars : HashMap<i32, IAvatar>,
    #[wasm_bindgen(skip)] pub game_map : GameMap
}


impl State {
    pub fn apply_tx_rev(&mut self, tx : &ITransaction) -> bool {
        if self.idx - 1 != tx.tx_id {
            panic!("Reversely apply: Transaction Idx must be state's idx - 1")
        }
        self.idx = tx.tx_id;
        match &tx.tx {
            Tx::Statement(_) => { false },
            Tx::Dice(dice) => {
                match &dice.result {
                    crate::def::transaction::DiceResult::DiceSanCheck(res) => {
                        let avatar = self.avatars.get_mut(&tx.avatar_id).unwrap();
                        let san_loss = match res.san_loss {
                            crate::def::transaction::DiceSanLoss::Success(v) => v,
                            crate::def::transaction::DiceSanLoss::Failure(v) => v,
                        };
                        avatar.detail.status.san += san_loss;
                        avatar.detail.status.san_loss -= san_loss;
                        true
                    },
                    _ => { false }
                }
            },
            Tx::UpdateAvatar { before, after } => {
                match (before, after) {
                    (Some(v), ..) => self.avatars.insert(v.id, v.clone()),
                    (None, Some(v)) => self.avatars.remove(&v.id),
                    _ => panic!("Bad Transaction! code = 3c10a28e")
                };
                true
            },
        }
    }

    pub fn apply_tx(&mut self, tx : &ITransaction) -> bool {
        if self.idx + 1 != tx.tx_id {
            panic!("Apply: Transaction Idx mut be state's idx + 1")
        }
        self.idx = tx.tx_id;
        match &tx.tx {
            Tx::Statement(_) => { false },
            Tx::Dice(dice) => {
                match &dice.result {
                    crate::def::transaction::DiceResult::DiceSanCheck(res) => {
                        let avatar = self.avatars.get_mut(&tx.avatar_id).unwrap();
                        let mut san_loss = match res.san_loss {
                            crate::def::transaction::DiceSanLoss::Success(v) => v,
                            crate::def::transaction::DiceSanLoss::Failure(v) => v,
                        };
                        if san_loss >= avatar.detail.status.san {
                            san_loss = avatar.detail.status.san;
                        }
                        avatar.detail.status.san -= san_loss;
                        avatar.detail.status.san_loss += san_loss;
                        true
                    },
                    _ => { false }
                }
            },
            Tx::UpdateAvatar { before , after } => {
                match (before, after) {
                    (Some(v), ..) => self.avatars.remove(&v.id),
                    (None, Some(v)) => self.avatars.insert(v.id, v.clone()),
                    _ => panic!("Bad Transaction! code = d2df489d")
                };
                true
            },
        }
    }
}

#[wasm_bindgen]
impl StateMachine {

    pub fn query_state(&self, idx : usize) -> Result<State, Left> {
        if idx > self.idx || idx < self.idx_base {
            return Err(Left {
                status : http::StatusCode::BAD_REQUEST.as_u16(),
                message : format!("query state out of range, idx range in ({}, {}), but query is {}", self.idx_base, self.idx, idx),
                uuid : "c4a28ada"
            });
        };
        let mut current_state = self.state.clone();
        let finite_state_len = self.idx - idx;
        for i in 1..=finite_state_len {
            let tx = &self.txs[self.idx - i];
            current_state.apply_tx_rev(tx);
        }
        Ok(current_state)
    }
    

}

impl StateMachine {
    pub fn observe<T>(
        &self, 
        idx_begin : Option<usize>, 
        idx_end : Option<usize>,
        observer : impl Fn(&State, &ITransaction) -> T
    ) -> Result<Vec<T>, Left> {
        let idx_end = match idx_end {
            Some(v) if v <= self.idx => { v },
            _ => self.idx
        };
        let idx_begin = match idx_begin {
            Some(v) if v >= self.idx_base => { v }, 
            _ => self.idx_base
        };
        let mut res : Vec<T> = Vec::with_capacity(idx_end - idx_begin + 1);
        let mut state = self.query_state(idx_end)?;
        res.push(observer(&state, &self.txs[idx_end - self.idx_base]));
        for i in (idx_begin..idx_end).rev() {
            let tx = &self.txs[i - self.idx_base];
            state.apply_tx_rev(tx);
            res.push(observer(&state, tx))
        }
        Ok(res)
    }

}

#[wasm_bindgen]
impl StateMachine {
    #[wasm_bindgen(constructor)]
    pub fn new_js(val : JsValue) -> Self {
        serde_wasm_bindgen::from_value(val).unwrap()
    }

    pub fn to_js(&self) -> JsValue {
        serde_wasm_bindgen::to_value(self).unwrap()
    }
    
    #[wasm_bindgen(js_name = "observe")]
    pub fn observe_js(
        &self,
        idx_begin : Option<usize>,
        idx_end : Option<usize>,
        f : &js_sys::Function
    ) -> Result<Vec<JsValue>, Left> {
        let this = JsValue::null();
        let rust_f = move |state : &State, tx : &ITransaction| -> JsValue {
            f.call2(&this, &serde_wasm_bindgen::to_value(state).unwrap(), &serde_wasm_bindgen::to_value(tx).unwrap()).unwrap()
        };
        self.observe(idx_begin, idx_end, rust_f)
    }
}

#[wasm_bindgen]
impl State {
    #[wasm_bindgen(constructor)]
    pub fn new_js(val : JsValue) -> Self {
        serde_wasm_bindgen::from_value(val).unwrap()
    }

    pub fn to_js(&self) -> JsValue {
        serde_wasm_bindgen::to_value(self).unwrap()
    }
}
