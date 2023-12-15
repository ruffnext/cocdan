import { createSignal, createContext, useContext, Accessor } from "solid-js";

export enum SupportedI18N {
  en_US = "en_US",
  zh_CN = "zh_CN"
}

const I18NContext = createContext<Accessor<SupportedI18N>>();


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
