use super::common::EraEnum;

pub enum WeaponRange {
    Melee,
    Meter(f64),
    Formula(String)     // unit is also meter
}

pub enum AmmoCapacity {
    None,
    Identity(u32),
    SingleUse
}

pub enum ExtraEffect {
    Burning,
    Stun
}

pub struct WeaponDamage {
    pub dice : String,            // Dice command like 1d3, 2d6, 4D6+2/2D6+1/1D4, etc...
    pub side_effect : ExtraEffect
}

pub struct Weapon {
    pub name : String,
    pub skill_name : String,
    pub damage : WeaponDamage,
    pub range : WeaponRange,
    pub penetration : bool,
    pub rate_of_fire : f32,     // num per round
    pub ammo_capacity : AmmoCapacity,
    pub reliability : u32,
    pub era : EraEnum,          // The same type of weapon may appear in different eras. 
                                // For this situation, please simply add a new record. 
                                // Because in different eras, even for the same type of weapon 
                                // (referring to having the same name here), its performance, 
                                // price, and various parameters may differ.
    pub price : f64,
    pub category : String,      // e.g. pistol, rifle, knife....
}
