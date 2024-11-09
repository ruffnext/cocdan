import { IAvatarLog } from "../../../../../../core/state/core"

type Props = {
  log: IAvatarLog
}

export default (props: Props) => {
  return (
    <div>
      <div class="flex items-center">
        <div class="w-8 h-8 bg-gray-300 rounded-full flex items-center justify-center">
          <img src={props.log.avatarHeader} class="h-6 w-6 rounded-full" />
        </div>
        <div class="text-sm ml-2">
          <div class="text-gray-800 font-bold">{props.log.avatarName}</div>
          <div class="text-gray-600">{props.log.message}</div>
        </div>
      </div>
    </div>
  )
}