import { createSignal, Show, For, Accessor } from "solid-js"
import { IAvatar } from "../../../../../bindings/entity/avatar/IAvatar"
import { IStage } from "../../../../../bindings/entity/basic/IStage"
import { post } from "../../../../../api/core"
import { StageWebsocket } from "../../../../../api/ws"
import { IGameLog } from "../../../../../core/state/core"
import Logs from "./Logs/Component"

type Props = {
  stage: IStage,
  avatar: IAvatar | undefined,
  allControllableAvatar: Array<IAvatar>
  onAvatarChange: (avatar: IAvatar) => void,
  stageWs: StageWebsocket,
  gameLogs: Accessor<Array<IGameLog>>
}

export default (props: Props) => {
  const minimumHeight = 48; // px
  let inputElement: HTMLTextAreaElement | undefined = undefined
  const [height, setHeight] = createSignal<number>(minimumHeight)
  const [input, setInput] = createSignal<string>("")
  const [isAvatarSelectorExtend, setIsAvatarSelectorExtend] = createSignal(false)
  function onInput(e: Event & {
    currentTarget: HTMLTextAreaElement;
  }) {
    const outerHeight = parseInt(window.getComputedStyle(e.currentTarget).height, 10);
    const clientHeight = e.currentTarget.clientHeight;
    const diff = outerHeight - clientHeight;
    e.currentTarget.style.height = '0';
    setInput(e.currentTarget.value)
    const newHeight = Math.min(Math.max(minimumHeight, e.currentTarget.scrollHeight + diff), 500);
    e.currentTarget.style.height = newHeight + 'px';
    setHeight(newHeight)
  }
  function onKeyPress(e: KeyboardEvent & {
    currentTarget: HTMLTextAreaElement;
  }) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      onSend()
      onInput(e)
      return
    }
  }
  async function onSend() {
    if (input() === "") {
      return
    }
    const avatar = props.avatar
    if (avatar == undefined) {
      console.warn("Avatar is not selected")
      return
    }
    const message = input()
    await post('/tx/:stage_id/role_play', { avatar_id: avatar.raw_id, text: message }, { stage_id: props.stage.raw_id.toString() }, true)

    if (inputElement) {
      inputElement.value = ""
    }
    setInput("")
  }
  return (
    <div class="w-full h-full overflow-y-auto">
      <Logs logs={props.gameLogs} allControllableAvatar={props.allControllableAvatar} height={height()}></Logs>
      <Show when={props.avatar !== undefined}>
        <div class="flex w-full min-h-12 text-lg mt-2 mb-2">
          <div class="w-4"></div>
          <button
            class="rounded-l-md border-solid border-l-2 border-t-2 border-b-2 border-r-0 text-nowrap pl-4 
                pr-4 bg-gray-100 h-full overflow-hidden hover:bg-green-300"
            style={`height: ${height()}px;`}
            on:click={() => setIsAvatarSelectorExtend(!isAvatarSelectorExtend())}>
            <div>
              {props.avatar!.version.name}
            </div>
          </button>
          <textarea ref={inputElement} class="flex-grow text-lg overflow-y-auto resize-none border-solid border-l-2 border-t-2 
            border-b-2 border-r-0 pl-4 pt-2 pb-2 pr-4 focus:border-r-2 focus:border-green-500 focus:outline-none"
            style="height: 48px;"
            on:input={onInput}
            on:keypress={onKeyPress}
            spellcheck={false}
          />
          <button class="border-solid border-t-2 border-b-2 w-12 h-12 hover:bg-green-300"
            style={`height: ${height()}px`}
            on:click={onSend}>
            <svg class="h-8 w-8 m-auto" width="24" height="24" viewBox="0 0 24 24" stroke-width="2"
              stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
              <path stroke="none" d="M0 0h24v24H0z" />
              <line x1="10" y1="14" x2="21" y2="3" />
              <path d="M21 3L14.5 21a.55 .55 0 0 1 -1 0L10 14L3 10.5a.55 .55 0 0 1 0 -1L21 3" />
            </svg>
          </button>
          <button class="h-12 w-12 border-solid border-r-2 border-t-2 border-b-2 border-l-0 rounded-r-md mr-2 hover:bg-green-300"
            style={`height: ${height()}px`}>
            <svg class="h-8 w-8 m-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
            </svg>
          </button>
        </div>
        <Show when={isAvatarSelectorExtend()}>
          <div class="z-20 absolute ml-4 flex flex-col-reverse max-h-96 min-w-32 rounded-md p-4 bg-white" style={`margin-bottom: ${height() + 8}px; box-shadow: rgba(0, 0, 0, 0.1) 0px -8px 15px -3px`}>
            <For each={props.allControllableAvatar} fallback={<div></div>}>
              {avatar => (
                <button class={`rounded-md w-full hover:bg-gray-200 text-lg ${props.avatar!.raw_id === avatar.raw_id ? "bg-green-300" : ""}`}
                  on:click={() => {
                    setIsAvatarSelectorExtend(false)
                    props.onAvatarChange(avatar)
                  }}>
                  {avatar.version.name}
                </button>
              )}
            </For>
          </div>
          <div class="absolute h-full w-full top-0 left-0" on:click={() => setIsAvatarSelectorExtend(false)}></div>
        </Show>
      </Show>
    </div>
  )
}