import { createContext, createSignal, useContext } from "solid-js";
import { ISession } from "../../bindings/entity/basic/ISession";
import { get } from "../../api/core";

function newContext() {
  const [session, setSession] = createSignal<ISession | "IsLoading" | "NotLoggedIn">("IsLoading")
  if (document.cookie.includes("SESSION")) {
    get('/user/me', undefined, false).then((ret) => {
      if ("Ok" in ret) {
        setSession(ret.Ok)
        return;
      }
      setSession("NotLoggedIn")
    })
  } else {
    setSession("NotLoggedIn")
  }
  return { session, setSession }
}

export const UserContext = createContext<ReturnType<typeof newContext>>()

export function SessionProvider(props: any) {
  const res = newContext()
  return (
    <UserContext.Provider value={res}>
      {props.children}
    </UserContext.Provider>
  )
}

export function useSession(): ReturnType<typeof newContext> { return useContext(UserContext) as any }
