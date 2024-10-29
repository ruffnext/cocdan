import { createContext, createSignal, useContext } from "solid-js";
import { ISession } from "../../bindings/entity/basic/ISession";
import { get } from "../../api/core";

function newContext() {
  const [user, setUser] = createSignal<ISession | undefined>()
  if (document.cookie.includes("SESSION")) {
    get('/user/me').then((ret) => {
      if ("Ok" in ret) {
        setUser(ret.Ok)
      }
    })
  }
  return { user, setUser }
}

export const UserContext = createContext<ReturnType<typeof newContext>>()

export function UserProvider(props: any) {
  const res = newContext()
  return (
    <UserContext.Provider value={res}>
      {props.children}
    </UserContext.Provider>
  )
}

export function useUser(): ReturnType<typeof newContext> { return useContext(UserContext) as any }
