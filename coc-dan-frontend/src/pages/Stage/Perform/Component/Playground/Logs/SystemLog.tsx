import { ISystemLog } from "../../../../../../core/state/core"

type Props = {
  log: ISystemLog
}

export default (props: Props) => {
  return (
    <div>
      <div class="text-gray-600 text-sm">
        {props.log.message}
      </div>
    </div>
  )
}