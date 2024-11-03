import { createSignal } from "solid-js"
import { IAvatar } from "../../../../../bindings/entity/avatar/IAvatar"
import { IStage } from "../../../../../bindings/entity/basic/IStage"
import { DOMElement } from "solid-js/jsx-runtime"

type Props = {
  stage: IStage,
  avatar: IAvatar
}

export default (props: Props) => {
  const minimumHeight = 48; // pex
  let inputElement: HTMLTextAreaElement | undefined = undefined
  const [height, setHeight] = createSignal<number>(minimumHeight)
  const [input, setInput] = createSignal<string>("")
  function onInput(e: Event & {
    currentTarget: HTMLTextAreaElement;
  }) {
    const outerHeight = parseInt(window.getComputedStyle(e.currentTarget).height, 10);
    const clientHeight = e.currentTarget.clientHeight;
    const diff = outerHeight - clientHeight;
    e.currentTarget.style.height = '0';
    setInput(e.currentTarget.value)
    const newHeight = Math.max(minimumHeight, e.currentTarget.scrollHeight + diff)
    e.currentTarget.style.height = newHeight + 'px';
    setHeight(e.currentTarget.scrollHeight + diff)
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
    if (inputElement) {
      inputElement.value = ""
    }
    console.log(input())
    setInput("")
  }
  return (
    <div class="flex flex-col-reverse w-full h-full overflow-y-auto">
      <div class="flex w-full min-h-12 text-lg mt-2 mb-2">
        <div class="w-4"></div>
        <button
          class="rounded-l-md border-solid border-l-2 border-t-2 border-b-2 border-r-0 text-nowrap pl-4 
                pr-4 bg-gray-100 h-full overflow-hidden hover:bg-green-300"
          style={`height: ${height()}px;`}
        >
          {props.avatar.name}
        </button>
        <textarea ref={inputElement} class="flex-grow text-lg overflow-y-hidden resize-none border-solid border-l-2 border-t-2 
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
    </div>
  )
}