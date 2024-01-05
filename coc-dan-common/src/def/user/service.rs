use ts_rs::TS;

#[derive(serde::Deserialize, Debug, TS)]
#[ts(export, rename = "IUserLogin", export_to = "bindings/user/service/IUserLogin.ts")]
pub struct IUserLogin {
    pub name : String
}
