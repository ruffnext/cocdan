import { useNavigate, useParams } from "@solidjs/router"
import { get, post } from "../../../api/core"
import { createEffect, createResource, createSignal, For, on } from "solid-js"
import { IStage } from "../../../bindings/entity/basic/IStage"
import { IAvatar } from "../../../bindings/entity/avatar/IAvatar"
import Playground from "./Component/Playground/Component"
import { Suspense, Show } from "solid-js"
import AvatarEditor from "./Component/AvatarEditor/Component"
import { deepClone } from "../../../core/utils"
import { useSession } from "../../Login/context"
import { GameState, IGameLog } from "../../../core/state/core"

export default () => {
  const param = useParams()
  const { session } = useSession()
  const navigate = useNavigate()
  const [isExtend, setIsExtend] = createSignal(true)
  const [editingAvatar, setEditingAvatar] = createSignal<[string, IAvatar] | ["Add", undefined] | [undefined, undefined]>([undefined, undefined])
  const [selectedAvatar, setSelectedAvatar] = createSignal<[string, IAvatar] | [undefined, undefined]>([undefined, undefined])
  const [gameLogs, setGameLogs] = createSignal<Array<IGameLog>>([])
  const gameState = new GameState(param['id'], (logs) => {
    setGameLogs(deepClone(logs).reverse())
  })

  createEffect(() => {
    if (session() == "NotLoggedIn") {
      navigate("/login")
    }
  })

  const [stage] = createResource(async (): Promise<IStage | undefined> => {
    const resp = await get('/stage/:id/get', { id: param['id'] }, true)
    if ("Ok" in resp) {
      await gameState.init()
      setGameLogs(deepClone(gameState.logs).reverse())
      return resp.Ok
    } else {
      return undefined
    }
  })

  const [avatars, { mutate, refetch }] = createResource(async (): Promise<Array<IAvatar> | undefined> => {
    const resp = await get('/stage/:id/my_avatars', { id: param['id'] }, true)
    if ("Ok" in resp) {
      if (!selectedAvatar()[1] && resp.Ok.length > 0) {
        const avatar = resp.Ok[0]
        setSelectedAvatar([avatar.raw_id, avatar])
      }
      return resp.Ok
    } else {
      return undefined
    }
  })

  createEffect(on(avatars, (avatars) => {
    if (selectedAvatar()[1] && avatars) {
      const avatar = avatars.find((a) => a.raw_id == selectedAvatar()[0])
      if (avatar) {
        setSelectedAvatar([avatar.raw_id, avatar])
      } else {
        setSelectedAvatar([undefined, undefined])
      }
    }
    if (editingAvatar()[0] && avatars) {
      const avatar = avatars.find((a) => a.raw_id == editingAvatar()[0])
      if (avatar) {
        setEditingAvatar([avatar.raw_id, avatar])
      } else {
        setEditingAvatar([undefined, undefined])
      }
    }
  }))

  const onAvatarDelete = (avatar: IAvatar) => {
    let nextAvatar: IAvatar | undefined = undefined
    if (avatar.raw_id == selectedAvatar()[0]) {
      const avatarsDeref = avatars()
      if (avatarsDeref) {
        nextAvatar = avatarsDeref.find((a) => a.raw_id != avatar.raw_id)
      }
      if (nextAvatar) {
        setSelectedAvatar([nextAvatar.raw_id, nextAvatar])
      } else {
        setSelectedAvatar([undefined, undefined])
      }
    }
    setEditingAvatar([undefined, undefined])
    refetch()
  }

  async function onRequestMoreLogs(): Promise<boolean> {
    await gameState.fetchMoreLogs()
    setGameLogs(deepClone(gameState.logs).reverse())
    return gameState.hasMoreLogs()
  }

  async function onAvatarAdd(avatar: IAvatar) {
    const resp = await post('/avatar/new', {
      stage_id: avatar.stage.raw_id,
      detail: avatar.version,
    }, undefined, true)
    if ("Ok" in resp) {
      const avatarsDeref = avatars()
      if (avatarsDeref) {
        mutate(avatarsDeref.concat([resp.Ok]))
        setEditingAvatar([resp.Ok.raw_id, resp.Ok])
      }
      if (!selectedAvatar()[0]) {
        setSelectedAvatar([resp.Ok.raw_id, resp.Ok])
      }
    }
  }

  async function onAvatarChange(avatar: IAvatar) {
    const resp = await post('/avatar/update', {
      raw_id: avatar.raw_id,
      detail: avatar.version,
    }, undefined, true)
    if ("Ok" in resp) {
      refetch()
    }
  }

  return (
    <main class={`bg-bg w-full h-full max-w-100vw text-textcolor flex`}>
      <button class={`absolute top-1 left-1 h-8 w-16 ${isExtend() ? "hidden" : ""} justify-center items-center flex bg-gray-200 rounded-md hover:bg-green-300`} on:click={() => setIsExtend(true)}>
        <svg class="h-8 w-8 text-black" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3" />
        </svg>
      </button>
      <div class={`h-full w-20 min-w-20 flex-col items-center bg-bgcolor text-textcolor shadow-lg relative rs-sidebar ${isExtend() ? "flex" : "hidden"}`}>
        <button class="flex h-8 w-16 hover:bg-green-300 rounded-md items-center justify-center mt-1 mb-1" on:click={() => setIsExtend(false)}>
          <svg class="h-8 w-8 text-black" width="24" height="24"
            viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none"
            stroke-linecap="round" stroke-linejoin="round">
            <path stroke="none" d="M0 0h24v24H0z" />
            <line x1="5" y1="12" x2="19" y2="12" />
            <line x1="5" y1="12" x2="9" y2="16" />
            <line x1="5" y1="12" x2="9" y2="8" /></svg>
        </button>
        <hr class="w-full" />
        <div class="flex items-center px-2 flex-col flex-1">
          <For each={avatars()} fallback={<div></div>}>
            {avatar => (
              <button class={`w-16 h-16 mt-2 mb-2 overflow-clip rounded-md border-solid border-4 hover:bg-green-300 p-1
                ${editingAvatar()[0] == avatar.raw_id ? "bg-green-300" : ""}
                ${selectedAvatar()[0] == avatar.raw_id ? "border-green-500" : "border-gray-500"}
                `}
                on:click={() => {
                  if (editingAvatar()[0] == avatar.raw_id) {
                    setEditingAvatar([undefined, undefined])
                  } else {
                    setEditingAvatar([avatar.raw_id, avatar])
                  }
                }}
              >
                {avatar.version.name}
              </button>
            )}
          </For>
          <button class={`w-12 h-12 mt-2 mb-2 hover:text-green-500 ${editingAvatar()[0] === "Add" ? "text-green-500" : "text-black"}`}
            on:click={() => {
              if (editingAvatar()[0] === "Add") {
                setEditingAvatar([undefined, undefined])
              } else {
                setEditingAvatar(["Add", undefined])
              }
            }}>
            <svg class="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v3m0 0v3m0-3h3m-3 0H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </button>
          <div class="flex-1"></div>
          <button class="w-12 h-12 mt-2 mb-2 text-black hover:text-green-500">
            <svg class="h-12 w-12"
              viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none"
              stroke-linecap="round" stroke-linejoin="round">
              <path stroke="none" d="M0 0h24v24H0z" />
              <path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 0 0 2.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 0 0 1.065 
                            2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 0 0 -1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 0 0 -2.572 
                            1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 0 0 -2.573 -1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 0 0 -1.065 
                            -2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 0 0 1.066 -2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
          </button>
          <button class="w-12 h-12 mt-2 mb-2 text-black hover:text-green-500">
            <svg class="h-12 w-12" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
              <path stroke="none" d="M0 0h24v24H0z" />
              <path d="M9 11l-4 4l4 4m-4 -4h11a4 4 0 0 0 0 -8h-1" />
            </svg>
          </button>
          <div class="h-4 w-full"></div>
        </div>
      </div>
      <Show when={editingAvatar()[0] && isExtend()}>
        <AvatarEditor
          avatar={editingAvatar() as any}
          stage={stage() as any}
          onChanged={onAvatarChange}
          onDelete={onAvatarDelete}
          onAdd={onAvatarAdd}
        />
      </Show>
      <Suspense fallback={<div>...</div>}>
        <Playground
          stage={stage() as any}
          avatar={selectedAvatar()[1]}
          allControllableAvatar={avatars() ? deepClone(avatars())!.reverse() : []}
          onAvatarChange={(avatar) => {
            setSelectedAvatar([avatar.raw_id, avatar])
          }}
          gameLogs={gameLogs}
          onRequestMoreLogs={onRequestMoreLogs}
        />
      </Suspense>
    </main>
  );
}