import { For, Show, createSignal } from "solid-js"
import "./dropdown.css"

interface Props {
  items: Array<{label : string, value : any}>,
  initialValue: string,
  setValue: (e: any) => string
}

export default (props: Props) => {
  const [val, setVal] = createSignal(props.initialValue)
  const [active, setActive] = createSignal(false)
  const update = (e: any) => {
    setVal(props.setValue(e))
    setActive(false)
  }
  const triggerDisplay = (show: boolean | undefined) => {
    if (show == undefined) {
      setActive(!active())
    } else {
      setActive(show)
    }
  }
  return (
    <div style="position : relative">
      <p class="trigger" aria-haspopup="true" aria-controls="dropdown-menu3"
        onClick={() => triggerDisplay(undefined)} >
        {val()}
      </p>
      <Show when={active()}>
        <div style="position : absolute; top : 100%; margin-top : 0.5em; z-index : 20; width: 6em" role="menu">
          <div class="dropdown-content" style="padding : 0">
            <For each={props.items}>
              {(item, _i) => {
                return (
                  <p class="my-dropdown-item" style="display : block;" onClick={() => update(item.value)}>{item.label}</p>
                )
              }}
            </For>
          </div>
        </div>
      </Show>
    </div>
  )
}