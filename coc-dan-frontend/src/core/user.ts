import { IStage } from "../bindings/IStage"
import { IUser } from "../bindings/IUser"
import { createSignal } from "solid-js"
import { get } from "../core"
import toast from "solid-toast"

export class User {
  raw : IUser
  constructor(raw : IUser) {
    this.raw = raw
  }
  async list_stages() : Promise<IStage[]> {
    try {
      const res : Array<IStage> = await get("/api/stage/my_stages", null, false)
      return res
    } catch (error : any) {
      console.log(error)
      if (error.status != 204) {
        toast.error(error.message)
      }
    }
    return []
  }
}
function newEmpty() {
  return new User({
    id : 1, // for development
    name : "developer",
    nick_name : "developer"
  })
}

export const [globalUser, setGlobalUser] = createSignal<User>(newEmpty());
