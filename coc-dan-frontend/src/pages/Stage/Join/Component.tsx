import { createAsync, useNavigate, useParams } from "@solidjs/router"
import { useSession } from "../../Login/context"
import { createEffect, createSignal, Match, Switch } from "solid-js"
import { IStage } from "../../../bindings/entity/basic/IStage"
import { get, post } from "../../../api/core"
import { ISession } from "../../../bindings/entity/basic/ISession"

export default () => {
  const param = useParams()
  const navigate = useNavigate()
  const [isJoined, setIsJoined] = createSignal(false)
  const { session, setSession } = useSession()
  const stage = createAsync<undefined | IStage | "Failed">(async () => {
    const resp = await get('/stage/:id', { id: param['id'] }, true)
    if ("Ok" in resp) {
      return resp.Ok
    } else {
      return "Failed"
    }
  })

  createEffect(async () => {
    const sessionContent = session();
    const stageContent = stage();
    if (sessionContent != "NotLoggedIn" && sessionContent != "IsLoading" && stageContent != "Failed" && stageContent != undefined) {
      const resp = await get('/stage/:id/is_joined', { id: param['id'] }, true)
      if ("Ok" in resp) {
        if (resp.Ok.is_joined) {
          setIsJoined(true)
        }
      }
    }
  })

  const loadingStage = () => {
    return (
      <div>loading</div>
    )
  }

  const loggedIn = (props: { session: ISession }) => {
    let name = ""
    if ("User" in props.session.session_type) {
      name = props.session.session_type.User.username
    }

    return (
      isJoined() ? (
        <button class="w-full h-10 rounded-md bg-green-300 hover:bg-green-500"
          on:click={() => navigate(`/stage/${param['id']}/perform`)}>
          Goto Stage
        </button>
      ) : (
        <button class="w-full h-10 rounded-md bg-green-300 hover:bg-green-500">
          Join Stage
        </button>
      )
    )
  }

  const notLoggedIn = () => {
    const [username, setUserName] = createSignal("")
    const joinStage = async () => {
      if (username() === "") {
        return
      }
      const registerResp = await post('/user/register', {
        username: username(),
        password: username(),
        nickname: username(),
      }, undefined, true)
      if ("Error" in registerResp) {
        return
      }
      const loginResp = await post('/user/login', {
        username: username(),
        password: username(),
      }, undefined, true)
      if ("Error" in loginResp) {
        return
      }
      setSession(loginResp.Ok)
      const joinResp = await post('/stage/:stage_id/join', undefined, { stage_id: param['id'] }, true)
      if ("Ok" in joinResp) {
        navigate(`/stage/${param['id']}/perform`)
      }
    }
    return (
      <div>
        <div class="mb-4">
          <label class="block text-gray-700 text-sm font-bold mb-2">
            How could we call you?
          </label>
          <input class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none"
            type="text"
            placeholder="Your name"
            on:change={(e) => setUserName(e.target.value)}
          />
          <button
            class="mt-4 w-full h-10 rounded-md bg-green-300 hover:bg-green-500"
            on:click={() => joinStage()}>
            Join Stage
          </button>
        </div>
      </div>
    )
  }

  const loadedStage = (props: { stage: IStage }) => {
    return (
      <div>
        <div class="w-full h-48 bg-gray-300 items-center justify-center">
          <p class="w-full text-center">stage cover</p>
        </div>
        <div class="w-full pl-8 pr-8 bg-white">
          <div>
            <span class="text-3xl font-bold">
              {props.stage.title}
            </span>
            <span class="text-sm pl-2">
              @ {props.stage.owner.username}
            </span>
            <p>
              {props.stage.description === "" ? "No description" : props.stage.description}
            </p>
          </div>
          <div class="pt-4 pb-4">
            <Switch>
              <Match when={session() != "NotLoggedIn" && session() != "IsLoading"}>
                {loggedIn({ session: session() as ISession })}
              </Match>
              <Match when={session() === "IsLoading"}>
                <div>Loading</div>
              </Match>
              <Match when={session() === "NotLoggedIn"}>
                {notLoggedIn()}
              </Match>
            </Switch>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div class="w-full h-full bg-gray-100">
      <div class="h-full content-center max-w-xs m-auto">
        <div class="bg-white shadow-md">
          <Switch>
            <Match when={!stage()}>
              {loadingStage()}
            </Match>
            <Match when={stage() == "Failed"}>
              <div>Failed</div>
            </Match>
            <Match when={stage() != undefined}>
              {loadedStage({ stage: stage() as any })}
            </Match>
          </Switch>
        </div>
      </div>
    </div>
  )
}