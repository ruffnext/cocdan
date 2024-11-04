import toast from "solid-toast";
import { IReqUserLogin } from "../bindings/api/user/login/IReqUserLogin"
import { IReqUserRegister } from "../bindings/api/user/register/IReqUserRegister"
import { ISession } from "../bindings/entity/basic/ISession"
import { IReqCreateStage } from "../bindings/api/stage/create/IReqCreateStage";
import { IStage } from "../bindings/entity/basic/IStage";
import { IAvatar } from "../bindings/entity/avatar/IAvatar";
import { IReqCreateAvatar } from "../bindings/api/avatar/create/IReqCreateAvatar";
import { IReqUpdateAvatar } from "../bindings/api/avatar/update/IReqUpdateAvatar";
import { IReqDeleteAvatar } from "../bindings/api/avatar/delete/IReqDeleteAvatar";

type PostApiKeys =
  '/user/login' |
  '/user/register' |
  '/user/logout' |
  '/stage/new' |
  '/avatar/new' |
  '/avatar/update' |
  '/avatar/delete'

type PostReqBodyType<Key extends PostApiKeys> =
  Key extends '/user/login' ? IReqUserLogin :
  Key extends '/user/register' ? IReqUserRegister :
  Key extends '/stage/new' ? IReqCreateStage :
  Key extends '/avatar/new' ? IReqCreateAvatar :
  Key extends '/avatar/update' ? IReqUpdateAvatar :
  Key extends '/avatar/delete' ? IReqDeleteAvatar :
  undefined;

type PostReqUrlType<Key extends PostApiKeys> =
  Key extends '' ? string :
  undefined

type ISimpleResponse = {
  message: string
}

type IApiResponse<T> = {
  "Ok": T
} | {
  "Error": ApiError
}

type PostReqRespType<Key extends PostApiKeys> =
  Key extends '/user/login' ? ISession :
  Key extends '/user/register' ? ISession :
  Key extends '/user/logout' ? ISimpleResponse :
  Key extends '/stage/new' ? IStage :
  Key extends '/avatar/new' ? IAvatar :
  Key extends '/avatar/update' ? IAvatar :
  Key extends '/avatar/delete' ? ISimpleResponse :
  undefined;

type ApiError = {
  status: 500,
  message: string,
  code: string | null
}

function url_format_base(url: string, params: Record<string, any> | undefined): string {
  if (!params) {
    return url
  }
  let new_url = url
  for (const key in params) {
    new_url = new_url.replace(`:${key}`, params[key])
  }
  return new_url
}

// function url_format_query(url: string, params: Record<string, any> | undefined): string {
//   if (params === undefined) {
//     return url
//   }

//   const searchParams = new URLSearchParams(params)
//   return `${url}?${searchParams.toString()}`
// }

export async function post<Key extends PostApiKeys>(
  key: Key,
  body: PostReqBodyType<Key>,
  url: PostReqUrlType<Key>,
  autoToast: boolean = true
): Promise<IApiResponse<PostReqRespType<Key>>> {
  const params = {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  }

  const req_url_base = "/api" + url_format_base(key, url)

  let this_error: ApiError = {
    status: 500,
    message: "unknown error",
    code: null
  }

  try {
    const res = await fetch(req_url_base, params)
    if (res.status == 200) {
      return { "Ok": await res.json() }
    } else {
      this_error.message = await res.text()
      if (this_error.message.startsWith("{")) {
        this_error = JSON.parse(this_error.message)
      }
    }
  } catch (error) {
    console.error(error)
  }

  if (autoToast) {
    toast.error(this_error.message)
  }

  return { "Error": this_error }
}


type GetApiKeys =
  '/user/me' |
  '/stage/:id' |
  '/stage/:id/my_avatars'

type GetRespType<Key extends GetApiKeys> =
  Key extends '/user/me' ? ISession :
  Key extends '/stage/:id' ? IStage :
  Key extends '/stage/:id/my_avatars' ? Array<IAvatar> :
  never;

type GetReqUrlType<Key extends GetApiKeys> =
  Key extends '/stage/:id' ? { id: string } :
  Key extends '/stage/:id/my_avatars' ? { id: string } :
  undefined;

export async function get<Key extends GetApiKeys>(
  key: Key,
  url: GetReqUrlType<Key>,
  autoToast: boolean = true
): Promise<IApiResponse<GetRespType<Key>>> {
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

  let req_url_base = "/api" + url_format_base(key, url)

  try {
    const res = await fetch(req_url_base, params)
    if (res.status == 200) {
      return { "Ok": await res.json() }
    } else {
      this_error.message = await res.text()
      if (this_error.message.startsWith("{")) {
        this_error = JSON.parse(this_error.message)
      }
    }
  } catch (error) {
    console.error(error)
  }

  if (autoToast) {
    toast.error(this_error.message)
  }

  return { "Error": this_error }
}