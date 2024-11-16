import { Accessor, createEffect, createSignal, For, Match, Show, Switch } from "solid-js"
import { IGameLog } from "../../../../../../core/state/core"
import AvatarLog from "./AvatarLog"
import SystemLog from "./SystemLog"
import { IAvatar } from "../../../../../../bindings/entity/avatar/IAvatar"
import { DOMElement } from "solid-js/jsx-runtime"

type Props = {
  logs: Accessor<Array<IGameLog>>
  onRequestMoreLogs: () => Promise<boolean>
  allControllableAvatar: Array<IAvatar>
  height: number
}

export default (props: Props) => {
  let controllableAvatars: Array<string> = []
  let container: HTMLDivElement | undefined = undefined;
  const [isFetchingLogs, setIsFetchingLogs] = createSignal(false)
  const [latestPxToTop, setLatestPxToTop] = createSignal(0)
  const [hasMoreLogs, setHasMoreLogs] = createSignal(true)
  createEffect(() => {
    controllableAvatars = props.allControllableAvatar.map(avatar => avatar.raw_id)
  })
  const onLogsScroll = (e: Event & {
    currentTarget: HTMLDivElement;
    target: DOMElement;
  }) => {
    const pxToTop = e.target.scrollHeight + e.target.scrollTop - e.target.clientHeight
    if (hasMoreLogs() && pxToTop < 400 && !isFetchingLogs() && latestPxToTop() > pxToTop) {
      setIsFetchingLogs(true)
      props.onRequestMoreLogs().then((hasMore) => {
        setHasMoreLogs(hasMore)
        setIsFetchingLogs(false)
      })
    }
    setLatestPxToTop(pxToTop)
  }

  return (
    <div
      ref={container}
      class="overflow-y-auto pl-8 pr-8 flex flex-col-reverse pt-8 pb-8 flex-grow-0"
      style={`height: calc(100% - ${props.height}px - 1em)`}
      on:scroll={onLogsScroll}
    >
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
      <Show when={isFetchingLogs()}>
        <div class="text-center text-gray-600">Fetching logs...</div>
      </Show>
      <Show when={!hasMoreLogs()}>
        <div class="text-center text-gray-600">No more logs</div>
      </Show>
    </div>
  )
}