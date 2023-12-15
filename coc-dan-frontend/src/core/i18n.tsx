import { createSignal, createContext, useContext, Accessor } from "solid-js";

export enum SupportedI18N {
  en_US = "en-US",
  zh_CN = "zh-CN"
}

const I18NContext = createContext<Accessor<SupportedI18N>>();
function getBrowserLanguage () : SupportedI18N {
  const language = navigator.language
  if (language == "en-US") {
    return SupportedI18N.en_US
  } else if (language == "zh-CN") {
    return SupportedI18N.zh_CN
  } else {
    return SupportedI18N.en_US
  }
}

export function I18nProvider (props : any) {
  const [i18n, _setI18n] = createSignal(props.i18n || SupportedI18N.zh_CN)
  return (
    <I18NContext.Provider value={i18n}>
      {props.children}
    </I18NContext.Provider>
  )
}

// @ts-ignore
export function useI18N() : Accessor<SupportedI18N> { return useContext(I18NContext) }
