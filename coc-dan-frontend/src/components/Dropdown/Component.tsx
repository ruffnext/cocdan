import { For, createSignal } from "solid-js"
import "./dropdown.css"

export type IDropdownItem = {label : string, value : any}
interface Props {
  items: Array<IDropdownItem>,
  initialLabel: string,
  setValue: (e: any) => string
}


export default (props: Props) => {
  const [val, setVal] = createSignal(props.initialLabel)
  const [active, setActive] = createSignal(false)
  const update = (e: any) => {
    setVal(props.setValue(e))
    setActive(false)
  }
  let dropdown : any;
  const triggerDisplay = (show: boolean | undefined) => {
    if (show == undefined) {
      setActive(!active())
    } else {
      setActive(show)
    }
    if (active() && dropdown != undefined) {
      dropdown.focus()
    }
  }

  return (
    <div style="position : relative; user-select : none">
      <p class="trigger" onClick={() => triggerDisplay(undefined)}>
        {val()}
      </p>
      <div ref={dropdown}
        style={active() ? "display : block;" : "display : none"} tabIndex={active() ? "-1" : ""} class="my-dropdown-menu"
          onBlur={() => triggerDisplay(false)}>
          <div class="dropdown-content" style="padding : 0; max-height : 20vh; overflow-y : auto">
            <For each={props.items}>
              {(item, _i) => {
                return (
                  <p class="my-dropdown-item" style="display : block;" onClick={() => update(item.value)}>{item.label}</p>
                )
              }}
            </For>
          </div>
        </div>
    </div>
  )
}
