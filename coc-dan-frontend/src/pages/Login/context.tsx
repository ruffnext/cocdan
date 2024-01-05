import { createContext, createSignal, useContext } from "solid-js";
import { get } from "../../core";
import Cookies from "js-cookie";
import { IUser } from "../../bindings/IUser";

export async function tryLogin() : Promise<IUser | undefined> {
  try {
    const res : IUser = await get("/api/user/me", null, false)
    return res
  } catch (error) {
    return undefined
  }
}

function newContext() {
  const [user, setUser] = createSignal<IUser | undefined>()
  if (document.cookie.includes("SESSION")) {
    tryLogin().then((e : IUser | undefined) => {
      if (e == undefined) {
        Cookies.remove("SESSION")
      } else {
        setUser(e)
      }
    })
  }
  return { user, setUser }
}

export const UserContext = createContext<ReturnType<typeof newContext>>()

export function UserProvider (props : any) {
  const res = newContext()
  return (
    <UserContext.Provider value={res}>
      { props.children }
    </UserContext.Provider>
  )
}

// @ts-ignore
export function useUser() : ReturnType<typeof newContext> { return useContext(UserContext) }
