import { createMemo } from "solid-js";
import * as i18n from "@solid-primitives/i18n";
import en_US from "./en_US"
import zh_CN from "./zh_CN"
import { SupportedI18N } from "../../../core/i18n";

const dictionaries = {
  "en_US" : en_US,
  "zh_CN" : zh_CN
};

export function getCardI18n (val : SupportedI18N) {
  const dict = createMemo(() => i18n.flatten(dictionaries[val]));

  return i18n.translator(dict);  
}