import * as i18n from "@solid-primitives/i18n";
import en_US from "./en-US";

export type IWeaponI18NRaw = typeof en_US;
export type IWeaponI18N = i18n.Flatten<IWeaponI18NRaw>
