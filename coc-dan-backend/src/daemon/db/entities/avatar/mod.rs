use crate::daemon::entities::TxAction;
use crate::utils::serde::optional_datetime_from_rfc3339;
use std::collections::HashMap;

use chrono::{DateTime, FixedOffset, Utc};
use serde_json::json;
use surrealdb::sql::{Datetime, Id, Thing};
use ts_rs::TS;

use crate::daemon::db::DbConn;
use crate::daemon::{DbEntity, SurrealRecord};
use crate::typedef::err::{ErrCode, Left};
use crate::{left_span, mls};

use super::common::EraEnum;
use super::skill::{OccupationalSkill, SkillAssigned};

use super::weapon::Weapon;
use super::{Stage, TxAux, User};

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IGender", export_to = "entity/avatar/IGender.d.ts")]
pub enum Gender {
    Other,
    Male,
    Female,
}

impl Default for Gender {
    fn default() -> Self {
        Self::Other
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Default, Debug, Clone)]
#[ts(
    export,
    rename = "IDescriptor",
    export_to = "entity/avatar/IDescriptor.d.ts"
)]
pub struct AvatarBasicInfo {
    age: u32,
    gender: Gender,
    homeland: String,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IMentalStatus",
    export_to = "entity/avatar/IMentalStatus.d.ts"
)]
pub enum MentalStatus {
    Lucid,
    Fainting,
    TemporaryInsanity,
    IndefiniteInsanity,
    PermanentInsanity,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IHealthStatus",
    export_to = "entity/avatar/IHealthStatus.d.ts"
)]
pub enum HealthStatus {
    Healthy,
    Ill,
    Injured,
    Critical,
    Dead,
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(export, rename = "IStatus", export_to = "entity/avatar/IStatus.d.ts")]
pub struct Status {
    pub hp: u32,
    pub mp: u32,
    pub san: u32,
    pub hp_loss: u32,
    pub mp_loss: u32,
    pub san_loss: u32,
    pub mental_status: MentalStatus,
    pub health_status: HealthStatus,
}

impl Default for Status {
    fn default() -> Self {
        Self {
            hp: 0,
            mp: 0,
            san: 0,
            hp_loss: 0,
            mp_loss: 0,
            san_loss: 0,
            mental_status: MentalStatus::Lucid,
            health_status: HealthStatus::Healthy,
        }
    }
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Default, Debug, Clone)]
#[ts(
    export,
    rename = "ICharacteristics",
    export_to = "entity/avatar/ICharacteristics.d.ts"
)]
pub struct Characteristics {
    pub str: u32,
    pub dex: u32,
    pub pow: u32,
    pub con: u32,
    pub app: u32,
    pub edu: u32,
    pub siz: u32,
    pub int: u32,
    pub mov: u32,
    pub luk: u32,
    pub mov_adj: Option<f32>,
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "ICharacteristicEnum",
    export_to = "entity/avatar/ICharacteristicEnum.d.ts"
)]
#[serde(rename_all = "lowercase")]
pub enum Characteristic {
    Str,
    Dex,
    Pow,
    Con,
    App,
    Edu,
    Siz,
    Int,
    Mov,
    Luk,
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "ICustomEquipment",
    export_to = "entity/avatar/ICustomEquipment.d.ts"
)]
pub struct CustomEquipment {
    pub description: String,
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IEquipmentItem",
    export_to = "entity/avatar/IEquipmentItem.d.ts"
)]
pub enum EquipmentItem {
    Weapon(Weapon),
    Custom(CustomEquipment),
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IEquipment",
    export_to = "entity/avatar/IEquipment.d.ts"
)]
pub struct Equipment {
    pub name: String,
    pub item: EquipmentItem,
}

#[derive(serde::Deserialize, serde::Serialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IAvatarDetail",
    export_to = "entity/avatar/IAvatarDetail.d.ts"
)]
pub struct AvatarDetail {
    pub name: String,
    pub header: String,
    pub status: Status,
    pub characteristics: Characteristics,
    pub basic_info: AvatarBasicInfo,
    pub skills: HashMap<String, SkillAssigned>,
    pub occupation: Occupation,
    pub equipments: Vec<Equipment>,
}

impl Default for AvatarDetail {
    fn default() -> Self {
        Self {
            name: "AvatarName".to_string(),
            header: "".to_string(),
            status: Default::default(),
            characteristics: Default::default(),
            basic_info: Default::default(),
            skills: Default::default(),
            occupation: Default::default(),
            equipments: Default::default(),
        }
    }
}

#[derive(serde::Serialize, serde::Deserialize, TS, PartialEq, Debug, Clone)]
#[ts(
    export,
    rename = "IOccupation",
    export_to = "entity/avatar/IOccupation.d.ts"
)]
pub struct Occupation {
    pub name: String,
    pub credit_rating: (u32, u32),
    pub era: EraEnum,
    pub characteristics: Vec<Characteristic>,
    pub occupational_skills: Vec<OccupationalSkill>,
}

impl Default for Occupation {
    fn default() -> Self {
        Self {
            name: "Custom".to_string(),
            credit_rating: (0, 100),
            era: EraEnum::None,
            characteristics: vec![Characteristic::Edu],
            occupational_skills: Vec::new(),
        }
    }
}

#[derive(serde::Deserialize, serde::Serialize, TS, Debug, Clone)]
#[ts(export, rename = "IAvatar", export_to = "entity/avatar/IAvatar.d.ts")]
pub struct Avatar {
    pub raw_id: String,
    pub stage: Stage,
    pub owner: User,
    pub version: AvatarDetail,
    #[serde(with = "optional_datetime_from_rfc3339")]
    #[ts(as = "Option<String>")]
    pub creation_time: Option<DateTime<FixedOffset>>,
}

#[derive(serde::Deserialize, serde::Serialize, Clone, Debug)]
pub struct AvatarDbAux {
    pub raw_id: String,
    pub stage: Thing,
    pub owner: Thing,
    pub version: AvatarDetail,
    pub creation_time: Option<Datetime>,
}

impl From<&Avatar> for AvatarDbAux {
    fn from(value: &Avatar) -> Self {
        Self {
            raw_id: value.raw_id.clone(),
            stage: value.stage.db_thing(),
            owner: value.owner.db_thing(),
            version: value.version.clone(),
            creation_time: Some(
                value
                    .creation_time
                    .map(|v| Datetime::from(v.to_utc()))
                    .unwrap_or(Datetime::from(Utc::now())),
            ),
        }
    }
}

impl DbEntity for AvatarDbAux {
    type IdType = String;

    fn db_id(&self) -> Id {
        Id::from(self.raw_id.clone())
    }

    fn db_tab_name() -> &'static str {
        "avatar"
    }
}

impl Avatar {
    pub fn db_tab_name() -> &'static str {
        "avatar"
    }

    pub async fn db_update(&self, version: Option<AvatarDetail>, db: &DbConn) -> Result<(), Left> {
        let mut tx = if let Some(version) = &version {
            TxAux::new(
                self.stage.raw_id.clone(),
                self.owner.raw_id.clone(),
                self.raw_id.clone(),
                TxAction::AvatarModify((
                    self.raw_id.clone(),
                    self.version.clone(),
                    version.clone(),
                )),
                db,
            )
            .await?
        } else {
            TxAux::new(
                self.stage.raw_id.clone(),
                self.owner.raw_id.clone(),
                self.raw_id.clone(),
                TxAction::AvatarDel((self.raw_id.clone(), self.version.clone())),
                db,
            )
            .await?
        };

        let statement = "
            BEGIN TRANSACTION;

            let $res = INSERT INTO avatar_version $version;

            let $tx_record = type::record($tx);

            UPDATE type::record($id) SET
                versions += {
                    time: $tx_record.time,
                    version: type::record(array::first($res.id)),
                    tx: $tx_record,
                };

            COMMIT TRANSACTION;
        ";

        let response = db
            .query(statement)
            .bind(json!({
                "id": Thing::from((Self::db_tab_name(), self.raw_id.as_str())).to_string(),
                "version": version,
                "tx": tx.id.to_string(),
            }))
            .await
            .and_then(|mut x| x.take::<Option<SurrealRecord>>(2))
            .map_err(mls!(ErrCode::DbError));

        match response {
            Ok(Some(_v)) => {
                tx.set_validate(db).await?;
                return Ok(());
            }

            _ => {
                tx.set_invalid(db).await?;
                return Err(left_span!(ErrCode::DbError));
            }
        };
    }

    pub async fn db_load_by_id(id: String, db: &DbConn) -> Result<Option<Self>, Left> {
        let query = "SELECT * FROM fn::load_version(type::record($id), time::now()) FETCH version, owner, stage, stage.owner;";
        let mut response = db
            .query(query)
            .bind(("id", Thing::from((Self::db_tab_name(), Id::from(id)))))
            .await
            .map_err(mls!(ErrCode::DbError))?;
        let res: Option<Self> = response.take(0).map_err(mls!(ErrCode::DbError))?;
        if let Some(v) = res.into_iter().next() {
            Ok(Some(v))
        } else {
            Ok(None)
        }
    }

    pub async fn db_del(id: String, db: &DbConn) -> Result<(), Left> {
        let avatar = if let Some(v) = Self::db_load_by_id(id.clone(), db).await? {
            v
        } else {
            return Ok(());
        };

        avatar.db_update(None, db).await?;

        Ok(())
    }
}
