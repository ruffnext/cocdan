import { createContext, useContext } from "solid-js";
import newEmpty from "../../core/card/new-empty";
import { IAvatar } from "../../bindings/IAvatar";
import * as i18n from "@solid-primitives/i18n";
import { Flatten } from "../../core/utils";
import { createStore } from "solid-js/store";

function newContext(params : IAvatar) {
  const raw : IAvatar = (params || newEmpty())
  const flattenAvatar = i18n.flatten(raw as any) as any
  const [avatar, setAvatar] = createStore<FlattenAvatar>(flattenAvatar)
  return { avatar, setAvatar }
}

export type FlattenAvatar = Flatten<IAvatar>
export const AvatarContext = createContext<ReturnType<typeof newContext>>();



export function AvatarProvider (props : any) {
  const res = newContext(props.avatar)
  return (
    <AvatarContext.Provider value={res}>
      {props.children}
    </AvatarContext.Provider>
  )
}

// @ts-ignore
export function useAvatar() : ReturnType<typeof newContext> { return useContext(AvatarContext) }
