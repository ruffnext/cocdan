import { listMyStages } from "../../../core/user"
import { useUser } from "../../Login/context"
import Dropdown, { IDropdownItem } from "../../../components/Dropdown/Component"
import { createEffect, createSignal } from "solid-js"
import { IStage } from "../../../bindings/IStage"
import { IUser } from "../../../bindings/IUser"
import { useAvatar } from "../context"

export default () => {
  const { user } = useUser()
  const { avatar } = useAvatar()
  const [stages, setStages] = createSignal<Record<string, IStage>>({})
  const [stagesDropdown, setStagesDropdown] = createSignal<Array<IDropdownItem>>([])
  async function queryStages(user : IUser | undefined) {
    if (user != undefined) {
      const stages = await listMyStages()
      const newStages : Record<string, IStage> = {}
      const newDropdowns : Array<IDropdownItem> = [{
        label : "None",
        value : "None"
      }]
      for (const key in stages) {
        newStages[key] = stages[key]
        newDropdowns.push({
          value : key,
          label : stages[key].title
        })
      }
      setStages(newStages)
      setStagesDropdown(newDropdowns)
    }
  }
  createEffect(() => {
    queryStages(user())
  })

  function isEditable() {
    const u = user()
    if (u == undefined) return false
    if (avatar.id == 0 || avatar.id == u.id) return true
    return false
  }

  return (
    <div>
      <button disabled={!isEditable()} class="button">Save</button>
      <Dropdown items={stagesDropdown()} initialLabel="None" setValue={(e) => {
        const s = stages()
        if (e in s) {
          return s[e].title
        } else {
          return "None"
        }
      }}/>
    </div>
  )
}
