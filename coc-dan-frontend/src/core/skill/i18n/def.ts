import * as i18n from "@solid-primitives/i18n";
import en_US from "./en-US";

export type ICardI18NRaw = typeof en_US;
export type ISkillI18N = i18n.Flatten<ICardI18NRaw>
