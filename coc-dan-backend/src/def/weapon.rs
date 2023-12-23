use super::common::EraEnum;
use ts_rs::TS;

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IWeaponRange", export_to = "bindings/weapon/IWeaponRange.ts")]
pub enum WeaponRange {
    Melee,
    Meter(f64),
    Formula(String)     // unit is also meter
}

impl Default for WeaponRange {
    fn default() -> Self {
        Self::Melee
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IAmmoCapacity", export_to = "bindings/weapon/IAmmoCapacity.ts")]
pub enum AmmoCapacity {
    None,
    Identity(u32),
    SingleUse
}

impl Default for AmmoCapacity {
    fn default() -> Self {
        Self::None
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IExtraEffect", export_to = "bindings/weapon/IExtraEffect.ts")]
pub enum ExtraEffect {
    Burning,
    Stun,
    None
}

impl Default for ExtraEffect {
    fn default() -> Self {
        Self::Burning
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default, Debug, Clone)]
#[ts(export, rename = "IWeaponDamage", export_to = "bindings/weapon/IWeaponDamage.ts")]
pub struct WeaponDamage {
    pub dice : String,            // Dice command like 1d3, 2d6, 4D6+2/2D6+1/1D4, etc...
    pub side_effect : Option<ExtraEffect>
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default, Debug, Clone)]
#[ts(export, rename = "IWeapon", export_to = "bindings/weapon/IWeapon.ts")]
pub struct Weapon {
    pub name : String,
    pub skill_name : String,
    pub damage : WeaponDamage,
    pub range : WeaponRange,
    pub impale : bool,
    pub rate_of_fire : f32,     // num per round
    pub ammo_capacity : AmmoCapacity,
    pub reliability : u32,
    pub era : EraEnum,          // The same type of weapon may appear in different eras. 
                                // For this situation, please simply add a new record. 
                                // Because in different eras, even for the same type of weapon 
                                // (referring to having the same name here), its performance, 
                                // price, and various parameters may differ.
    pub price : f64,            // USD
    pub category : Vec<String>, // e.g. improvised, basic, pistol, rifle.... use for search
}

#[cfg(test)]
mod tests {
    use crate::def::weapon::Weapon;

    #[test]
    fn test_parse() {
        let s = include_str!("../../resource/weapons.json");
        let weapons : Vec<Weapon> = serde_json::from_str(s).unwrap();
        println!("{:#?}", weapons);
    }
}
