import { Accessor, For } from "solid-js"
import { IGameLog } from "../../../../../../core/state/core"
import AvatarLog from "./AvatarLog"
import SystemLog from "./SystemLog"

type Props = {
  logs: Accessor<Array<IGameLog>>
}

export default (props: Props) => {
  return (
    <For each={props.logs()}>
      {log => {
        if ("AvatarLog" in log) {
          return <AvatarLog log={log.AvatarLog} />
        } else if ("SystemLog" in log) {
          return <SystemLog log={log.SystemLog} />
        } else {
          return <div>Unknown log</div>
        }
      }}
    </For>
  )
}