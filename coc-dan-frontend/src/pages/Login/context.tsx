import { createContext, createSignal, useContext } from "solid-js";
import { User } from "../../core/user";
import { IUser } from "../../bindings/IUser";
import { get } from "../../core";
import Cookies from "js-cookie";

export async function tryLogin() : Promise<User | undefined> {
  try {
    const res : IUser = await get("/api/user/me", null, false)
    return new User(res)
  } catch (error) {
    return undefined
  }
}

function newContext() {
  const [user, setUser] = createSignal<User | undefined>()
  if (document.cookie.includes("SESSION")) {
    tryLogin().then((e : User | undefined) => {
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
