import { IAvatarLog } from "../../../../../../core/state/core"

type Props = {
  log: IAvatarLog,
  isAvatarControllable: boolean
}

export default (props: Props) => {
  return (
    <div class={`flex items-center mb-2 mt-2 ${props.isAvatarControllable ? "flex-row-reverse" : ""}`}>
      <div class="w-12 h-12 bg-gray-300 rounded-full flex items-center justify-center">
        <img src={props.log.avatarHeader === "" ? "/img/default_avatar_header.png" : props.log.avatarHeader} class="h-12 w-12 rounded-full" />
      </div>
      <div class={`text-sm ${props.isAvatarControllable ? "mr-4" : "ml-4"}`}>
        <div class={`text-gray-800 font-bold text-lg ${props.isAvatarControllable ? "text-right" : ""}`}>{props.log.avatarName}</div>
        <div class="text-gray-600 text-wrap">{props.log.message}</div>
      </div>
    </div>
  )
}
