import { IAvatar } from "../../../../../bindings/entity/avatar/IAvatar"

type Props = {
  avatar: IAvatar
}

export default (props: Props) => {
  return (
    <div class="h-full w-80 bg-gray-300">
      {props.avatar.name}
    </div>
  )
}