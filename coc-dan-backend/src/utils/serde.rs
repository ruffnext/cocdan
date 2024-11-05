use serde::Serializer;
use surrealdb::sql::Thing;

pub mod datetime_from_rfc3339 {
    use chrono::{DateTime, FixedOffset};
    use serde::{Deserialize, Deserializer, Serialize, Serializer};

    pub fn deserialize<'de, D>(deserializer: D) -> Result<DateTime<FixedOffset>, D::Error>
    where
        D: Deserializer<'de>,
    {
        let s = String::deserialize(deserializer)?;
        DateTime::parse_from_rfc3339(&s).map_err(serde::de::Error::custom)
    }

    pub fn serialize<S: Serializer>(
        val: &DateTime<FixedOffset>,
        serializer: S,
    ) -> Result<S::Ok, S::Error> {
        val.to_rfc3339().serialize(serializer)
    }
}

pub mod optional_datetime_from_rfc3339 {
    use chrono::{DateTime, FixedOffset};
    use serde::{Deserialize, Deserializer, Serialize, Serializer};

    pub fn deserialize<'de, D>(deserializer: D) -> Result<Option<DateTime<FixedOffset>>, D::Error>
    where
        D: Deserializer<'de>,
    {
        let s = Option::<String>::deserialize(deserializer)?;
        if let Some(s) = s {
            if let Ok(v) = DateTime::parse_from_rfc3339(&s) {
                return Ok(Some(v));
            }
        }
        return Ok(None);
    }

    pub fn serialize<S: Serializer>(
        val: &Option<DateTime<FixedOffset>>,
        serializer: S,
    ) -> Result<S::Ok, S::Error> {
        match val {
            Some(val) => val.to_rfc3339().serialize(serializer),
            None => None::<String>.serialize(serializer),
        }
    }
}

pub fn thing_as_string<S: Serializer>(x: &Thing, s: S) -> Result<S::Ok, S::Error> {
    s.serialize_str(x.to_string().as_str())
}
