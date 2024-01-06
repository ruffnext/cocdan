import { listMyStages } from "../../../core/user"
import { useUser } from "../../Login/context"
import Dropdown, { IDropdownItem } from "../../../components/Dropdown/Component"
import { createSignal } from "solid-js"

interface Props {
  editable : boolean
}

export default (props : Props) => {
  const { user } = useUser()
  const [dropdownItems, setDropdownItems] = createSignal<Array<IDropdownItem>>([])
  async function queryStages() {
    if (user() != undefined) {
      const stages = await listMyStages()
      const res : Array<IDropdownItem> = []
      for (const item of stages) {
        res.push({
          label : item.title,
          value : item.uuid
        })
        setDropdownItems(res)
      }
    }
  }
  queryStages()
  return (
    <div>
      <button disabled={user() == undefined || props.editable === false} class="button">Save</button>
      <Dropdown items={dropdownItems()} initialLabel="默认" setValue={(e) => {return ""}}/>
    </div>
  )
}
