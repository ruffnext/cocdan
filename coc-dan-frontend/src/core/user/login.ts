import { get } from "../../core";
import { IUser, User } from "../user";

export async function try_login() : Promise<User | null> {
  try {
    const user : IUser = await get("/api/user/me", null, false)
    return new User(user)
  } catch (error) {
    return null
  }
}
