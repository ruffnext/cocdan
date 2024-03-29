import { createContext, useContext } from "solid-js";
import newEmpty from "../../core/card/new-empty";
import { IAvatar } from "../../bindings/IAvatar";
import { createStore } from "solid-js/store";

function newContext(params : IAvatar) {
  const raw : IAvatar = (params || newEmpty())
  const [avatar, setAvatar] = createStore<IAvatar>(raw)
  return { avatar, setAvatar }
}

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
