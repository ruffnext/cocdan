import { useParams } from "@solidjs/router"
import { get } from "../../../api/core"
import { createResource, createSignal, For } from "solid-js"
import { IStage } from "../../../bindings/entity/basic/IStage"
import { IAvatar } from "../../../bindings/entity/avatar/IAvatar"
import Playground from "./Component/Playground/Component"
import { Suspense, Show } from "solid-js"
import AvatarEditor from "./Component/AvatarEditor/Component"

export default () => {
  const param = useParams()
  const [isExtend, setIsExtend] = createSignal(true)
  const [editingAvatar, setEditingAvatar] = createSignal<IAvatar | "Add" | undefined>(undefined)
  const [selectedAvatar, setSelectedAvatar] = createSignal<IAvatar | undefined>(undefined)

  const [stage] = createResource(async (): Promise<IStage | undefined> => {
    const resp = await get('/stage/:id', { id: param['id'] }, true)
    if ("Ok" in resp) {
      return resp.Ok
    } else {
      return undefined
    }
  })

  const [avatars, { refetch }] = createResource(async (): Promise<Array<IAvatar> | undefined> => {
    const resp = await get('/stage/:id/my_avatars', { id: param['id'] }, true)
    if ("Ok" in resp) {
      if (resp.Ok.length > 0) {
        setSelectedAvatar(resp.Ok[0])
      }
      return resp.Ok
    } else {
      return undefined
    }
  })


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
              <button class={`w-16 h-16 mt-2 mb-2 rounded-md border-solid border-4 hover:bg-green-300 p-1
                ${editingAvatar() == avatar ? "bg-green-300" : ""}
                ${selectedAvatar() == avatar ? "border-green-500" : "border-gray-500"}
                `}
                on:dblclick={() => {
                  if (editingAvatar() == avatar) {
                    setEditingAvatar(undefined)
                  } else {
                    setEditingAvatar(avatar)
                  }
                }}
                on:click={() => {
                  setSelectedAvatar(avatar)
                }}
              >
                {avatar.name}
              </button>
            )}
          </For>
          <button class={`w-12 h-12 mt-2 mb-2 hover:text-green-500 ${editingAvatar() === "Add" ? "text-green-500" : "text-black"}`}
            on:click={() => {
              if (editingAvatar() === "Add") {
                setEditingAvatar(undefined)
              } else {
                setEditingAvatar("Add")
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
      <Show when={editingAvatar() && isExtend()}>
        <AvatarEditor
          avatar={editingAvatar() as any}
          stage={stage() as any}
          onChanged={() => {
            refetch()
          }}
        />
      </Show>
      <Suspense fallback={<div>...</div>}>
        {
          selectedAvatar() ? <Playground stage={stage() as any} avatar={selectedAvatar() as any} /> : <div></div>
        }
      </Suspense>
    </main>
  );
}