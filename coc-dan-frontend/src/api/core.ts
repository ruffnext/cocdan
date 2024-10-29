import toast from "solid-toast";
import { IReqUserLogin } from "../bindings/api/user/login/IReqUserLogin"
import { IReqUserRegister } from "../bindings/api/user/register/IReqUserRegister"
import { ISession } from "../bindings/entity/basic/ISession"

type PostApiKeys =
  '/user/login' |
  '/user/register' |
  '/user/logout';

type PostReqBodyType<Key extends PostApiKeys> =
  Key extends '/user/login' ? IReqUserLogin :
  Key extends '/user/register' ? IReqUserRegister :
  Key extends '/user/logout' ? null :
  never;

type PostReqUrlType<Key extends PostApiKeys> =
  Key extends '' ? string :
  undefined

type PostReqRespType<Key extends PostApiKeys> =
  Key extends '/user/login' ? ISession :
  Key extends '/user/register' ? ISession :
  Key extends '/user/logout' ? string :
  never;

type ApiError = {
  status: 500,
  message: string,
  code: string | null
}

export async function post<Key extends PostApiKeys>(
  key: Key,
  body: PostReqBodyType<Key>,
  url: PostReqUrlType<Key>,
  autoToast: boolean = true
): Promise<{
  "Ok": PostReqRespType<Key>
} | {
  "Error": ApiError
}> {
  const params = {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  }

  let this_error: ApiError = {
    status: 500,
    message: "unknown error",
    code: null
  }

  try {
    const res = await fetch(key, params)
    if (res.status == 200) {
      return { "Ok": await res.json() }
    } else {
      this_error.message = await res.text()
      this_error = JSON.parse(this_error.message)
    }
  } catch (error) {
  }

  if (autoToast) {
    toast.error(this_error.message)
  }

  return { "Error": this_error }
}


type GetApiKeys =
  '/user/me'

type GetRespType<Key extends GetApiKeys> =
  Key extends '/user/me' ? ISession :
  never;

export async function get<Key extends GetApiKeys>(
  key: Key,
  autoToast: boolean = true
): Promise<{
  "Ok": GetRespType<Key>
} | {
  "Error": ApiError
}> {
  const params = {
    method: "GET",
    headers: {
      "Content-Type": "application/json"
    }
  }

  let this_error: ApiError = {
    status: 500,
    message: "unknown error",
    code: null
  }

  try {
    const res = await fetch(key, params)
    if (res.status == 200) {
      return { "Ok": await res.json() }
    } else {
      this_error.message = await res.text()
      this_error = JSON.parse(this_error.message)
    }
  } catch (error) {
  }

  if (autoToast) {
    toast.error(this_error.message)
  }

  return { "Error": this_error }
}