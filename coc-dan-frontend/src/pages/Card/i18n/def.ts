import * as i18n from "@solid-primitives/i18n";
import en_US from "./en_US";

export type ICardI18NRaw = typeof en_US;
export type ICardI18N = i18n.Flatten<ICardI18NRaw>
