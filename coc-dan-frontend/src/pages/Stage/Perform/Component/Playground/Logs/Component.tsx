import { Accessor, createEffect, For, Match, Switch } from "solid-js"
import { IGameLog } from "../../../../../../core/state/core"
import AvatarLog from "./AvatarLog"
import SystemLog from "./SystemLog"
import { IAvatar } from "../../../../../../bindings/entity/avatar/IAvatar"

type Props = {
  logs: Accessor<Array<IGameLog>>
  allControllableAvatar: Array<IAvatar>
  height: number
}

export default (props: Props) => {
  let controllableAvatars: Array<string> = []
  let container: HTMLDivElement | undefined = undefined;
  createEffect(() => {
    controllableAvatars = props.allControllableAvatar.map(avatar => avatar.raw_id)
  })

  return (
    <div ref={container} class="overflow-y-auto pl-8 pr-8 flex flex-col-reverse pt-8 pb-8 flex-grow-0"
      style={`height: calc(100% - ${props.height}px - 1em)`}>
      <Switch>
        <Match when={props.logs().length === 0}>
          <div class="text-center text-gray-600">No logs</div>
        </Match>
        <Match when={container}>
          <For each={props.logs()}>
            {log => {
              if ("AvatarLog" in log) {
                return <AvatarLog log={log.AvatarLog} isAvatarControllable={
                  controllableAvatars.includes(log.AvatarLog.avatarId)
                } />
              } else if ("SystemLog" in log) {
                return <SystemLog log={log.SystemLog} />
              } else {
                return <div>Unknown log</div>
              }
            }}
          </For>
        </Match>
      </Switch>
    </div>
  )
}