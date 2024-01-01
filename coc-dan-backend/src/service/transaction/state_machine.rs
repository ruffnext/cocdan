use std::collections::HashMap;

use crate::{def::{transaction::{Tx, ITransaction}, avatar::IAvatar, GameMap}, err::Left};


#[derive(serde::Serialize, serde::Deserialize, PartialEq, Default, Debug, Clone)]
pub struct StateMachine {
    pub idx : usize,        // current maximum transaction id, and also total size of txs
    pub idx_base : usize,   // idx_base is included, minimum is 1
    pub txs : Vec<ITransaction>,
    pub state : State
}

#[derive(serde::Serialize, serde::Deserialize, PartialEq, Default, Debug, Clone)]
pub struct State {
    pub idx : usize,
    pub avatars : HashMap<i32, IAvatar>,
    pub game_map : GameMap
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
            Tx::UpdateAvatar { before, .. } => {
                self.avatars.insert(before.id, before.clone());
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
            Tx::UpdateAvatar { after , .. } => {
                self.avatars.insert(after.id, after.clone());
                true
            },
        }
    }
}

impl StateMachine {
    pub fn query_state(&self, idx : usize) -> Result<State, Left> {
        if idx > self.idx || idx < self.idx_base {
            return Err(Left {
                status : http::StatusCode::BAD_REQUEST,
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

    pub fn build_states<'a>(&'a self, idx_begin : Option<usize>, idx_end : Option<usize>) -> Result<Vec<(State, ITransaction)>, Left> {
        let idx_end = match idx_end {
            Some(v) if v <= self.idx => { v },
            _ => self.idx
        };
        let idx_begin = match idx_begin {
            Some(v) if v >= self.idx_base => { v }, 
            _ => self.idx_base
        };
        let mut res : Vec<(State, ITransaction)> = Vec::with_capacity(idx_end - idx_begin + 1);
        let mut state = self.query_state(idx_end)?;
        res.push((state.clone(), (&self.txs[idx_end - self.idx_base]).clone()));
        for i in (idx_begin..idx_end).rev() {
            let tx = &self.txs[i - self.idx_base];
            state.apply_tx_rev(tx);
            res.push((state.clone(), tx.clone()));
        }
        Ok(res)
    }
}