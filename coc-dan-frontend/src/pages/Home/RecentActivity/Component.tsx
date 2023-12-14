import { createSignal } from "solid-js"
import "./style.css"

enum TableEnum {
  Timeline,
  Stages
}

export default () => {
  const [selected, setSelected] = createSignal<TableEnum>(TableEnum.Timeline)
  return (
    <div id="user-recent-card" class="main-card-container radius">
      <div id="recent-toolbar">
        <p class="recent-toolbar-item recent-toolbar-item-begin" 
          classList={{ selected : selected() === TableEnum.Timeline }}
          onClick={() => {setSelected(TableEnum.Timeline)}}>
            Timeline
        </p>
        <p class="recent-toolbar-item" 
          classList={{ selected : selected() === TableEnum.Stages }}
          onClick={() => {setSelected(TableEnum.Stages)}}>
          Stages
        </p>
      </div>
    </div>
  )
}
