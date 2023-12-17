import { Flatten, unflatten } from "flatten-type"
import { IAvatar } from "../../../../bindings/IAvatar"
import { ISkillAssigned } from "../../../../bindings/avatar/ISkillAssigned"
import { SetStoreFunction } from "solid-js/store"
import { ISkillCategory } from "../../../../bindings/avatar/ISkillCategory"
import { getOccupationAvailableSkillCategories } from "../../../../core/skill/core"



export function setSkill (item : ISkillAssigned, avatar : Flatten<IAvatar>, setAvatar : SetStoreFunction<Flatten<IAvatar>>) : boolean {
  const total = item.occupation_skill_point + item.initial + item.interest_skill_point
  if (total < 0) return false
  if (total > 99) return false
  const res = item.occupation_skill_point
    // @ts-ignore
  if (item.name == "Credit Rating" && total < avatar["detail.occupation.credit_rating.0"]) return false
    // @ts-ignore
  if (item.name == "Credit Rating" && total > avatar["detail.occupation.credit_rating.1"]) return false
  if (res < 0) return false

  const raw : IAvatar = unflatten(avatar)
  var found = false
  for (const key in raw.detail.skills) {
    const val = raw.detail.skills[key]
    if (val.name == item.name) {
      raw.detail.skills[key].occupation_skill_point = res
      found = true
      break
    }
  }
  if (found == false) {
    // assign a new occupational skill
    if (item.assign_type == "AdditionalOccupational") {

      // initialize total available skill slot
      const available : Map<ISkillCategory, number> = getOccupationAvailableSkillCategories(raw.detail.occupation)

      // minus used
      for (const key in raw.detail.skills) {
        const val = raw.detail.skills[key]
        if (val.assign_type == "AdditionalOccupational") {
          const prev = available.get(val.category)
          if (prev != undefined) {
            available.set(val.category, Math.max(prev - 1, 0))
          }
        }
      }
      const res = available.get(item.category)
      if (res == undefined || res <= 0) {
        const extra = available.get("Any")
        if (extra == undefined || extra == 0) {
          return false
        } else {
          item.category = "Any"
        }
      }
    }
    raw.detail.skills[item.name] = item
  }

  const prefix = "detail.skills." + item.name + "."

  if (found) {
    // @ts-ignore
    setAvatar(prefix + "occupation_skill_point", res)
  } else {
    const merge : any = {}
    merge[prefix + "name"] = item.name
    merge[prefix + "assign_type"] = item.assign_type
    merge[prefix + "era"] = item.era
    merge[prefix + "initial"] = item.initial
    merge[prefix + "interest_skill_point"] = item.interest_skill_point
    merge[prefix + "occupation_skill_point"] = item.occupation_skill_point
    merge[prefix + "category"] = item.category
    setAvatar(merge)
  }
  return true
}

export function removeSkill (item : ISkillAssigned, _avatar : Flatten<IAvatar>, setAvatar : SetStoreFunction<Flatten<IAvatar>>) : boolean {
  if (item.assign_type == "Occupational") {
    return false
  } else {
    const prefix = "detail.skills." + item.name + "."
    if (item.assign_type == "AdditionalOccupational" && item.interest_skill_point != 0) {
      // @ts-ignore
      setAvatar(prefix + "assign_type", "Interest")
    } else {
      const merge : any = {}
      merge[prefix + "name"] = undefined
      merge[prefix + "assign_type"] = undefined
      merge[prefix + "era"] = undefined
      merge[prefix + "initial"] = undefined
      merge[prefix + "interest_skill_point"] = undefined
      merge[prefix + "occupation_skill_point"] = undefined
      merge[prefix + "category"] = undefined
      setAvatar(merge)
    }
    return true
  }
}
