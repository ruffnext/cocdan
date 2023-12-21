import { createMemo } from "solid-js";
import * as i18n from "@solid-primitives/i18n";
import en_US from "./en-US"
import zh_CN from "./zh-CN"
import { SupportedI18N } from "../../../core/i18n";

const dictionaries = {
  "en-US" : en_US,
  "zh-CN" : zh_CN
};

export type IWeaponDictionary = typeof en_US
export type IWeaponTranslator = i18n.Translator<i18n.Flatten<IWeaponDictionary>>

export function getWeaponI18n (val : SupportedI18N) {
  const dict = createMemo(() => i18n.flatten(dictionaries[val]));

  return i18n.translator(dict);  
}

export function getWeaponName (name: string, translator : IWeaponTranslator): string {
  // @ts-ignore
  const res = translator("weapon." + name + ".name")
  if (res == undefined) {
    return name
  } else {
    // @ts-ignore
    return res
  }
}
