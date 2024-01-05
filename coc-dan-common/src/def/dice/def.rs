use std::num::ParseIntError;
use rand::prelude::*;

#[derive(Debug)]
pub struct SimpleDice {
    pub dice_num : u32,
    pub side_num : u32,
}

impl TryFrom<&str> for SimpleDice {
    type Error = ParseIntError;
    fn try_from(value: &str) -> Result<SimpleDice, Self::Error> {
        let lower_case = value.to_lowercase();
        let res : Vec<&str>  = lower_case.split("d").collect();
        let dice_num : u32 = res[0].parse::<u32>()?;
        let side_num : u32 = res[1].parse::<u32>()?;
        Ok(Self { dice_num, side_num })
    }
}

impl SimpleDice {
    pub fn dice(&self) -> u32 {
        let mut rng = thread_rng();
        let mut res : u32 = 0;
        for _ in 0..self.dice_num {
            res += rng.gen_range(1..=self.side_num);
        }
        return res
    }
}
