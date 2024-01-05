import { IAvatar } from "../../bindings/IAvatar";
import { get } from "../../core";

export async function queryAvatarById(id : number) : Promise<IAvatar | undefined> {
  try {
    const res : IAvatar = await get("/api/avatar/" + id, null, false)
    return res
  } catch (error) {
    return undefined
  }
}
