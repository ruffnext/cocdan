import toast from "solid-toast"

export interface ServerError {
  uuid : string
  message : string
}

export async function post<T>(url : string, body : any | null, autoToast = true) : Promise<T> {
  var params : any = {
    method : "POST"
  }
  if (body != null) {
    params['headers'] = {
      "Content-Type" : "application/json"
    }
    params['body'] = JSON.stringify(body)
  }
  const res = await fetch(url, params)
  if (res.status == 200) {
    return await res.json()
  } else {
    var val : ServerError = {
      message : await res.text(),
      uuid : ""
    }
    try {
      val = await JSON.parse(val.message)
    } catch (error) {
    }
    if (val.message == "") {
      val.message = "unknown error"
    }
    if (autoToast) {
      toast.error(val.message)
    }
    throw val
  }
}

export async function get<T>(url : string, body : any | null, autoToast = true) : Promise<T> {
  var params : any = {
    method : "GET"
  }
  if (body != null) {
    params['headers'] = {
      "Content-Type" : "application/json"
    }
    params['body'] = JSON.stringify(body)
  }
  const res = await fetch(url, params)
  if (res.status == 200) {
    return await res.json()
  } else if (res.status == 201) {
    throw {
      message : "no content",
      uuid : ""
    }
  } else {
    var val : ServerError = {
      message : await res.text(),
      uuid : ""
    }
    try {
      val = await JSON.parse(val.message)
    } catch (error) {
    }
    if (val.message == "") {
      val.message = "unknown error"
    }
    if (autoToast) {
      toast.error(val.message)
    }
    throw val
  }
}