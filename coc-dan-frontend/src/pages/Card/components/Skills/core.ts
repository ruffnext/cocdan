import { Flatten, unflatten } from "flatten-type"
import { IAvatar } from "../../../../bindings/IAvatar"
import { ISkillAssigned } from "../../../../bindings/avatar/ISkillAssigned"
import { SetStoreFunction } from "solid-js/store"

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
    if (item.assign_type == "AdditionalOccupational") {
      var assigned = 0
      for (const key in raw.detail.skills) {
        if (raw.detail.skills[key].assign_type == "AdditionalOccupational") {
          assigned ++
        }
      }
      if (assigned >= raw.detail.occupation.additional_skill_num) {
        return false
      }
    }
    raw.detail.skills[item.name] = item
  }

  // @ts-ignore
  setAvatar("detail.skills." + item.name + ".occupation_skill_point", res)
  if (!found) {
    // @ts-ignore
    setAvatar("detail.skills." + item.name + ".name", item.name)
    // @ts-ignore
    setAvatar("detail.skills." + item.name + ".initial", item.initial)
    // @ts-ignore
    setAvatar("detail.skills." + item.name + ".era", item.era)
    // @ts-ignore
    setAvatar("detail.skills." + item.name + ".interest_skill_point", item.interest_skill_point)
    // @ts-ignore
    setAvatar("detail.skills." + item.name + ".assign_type", item.assign_type)
  }
  return true
}

export function removeSkill (item : ISkillAssigned, _avatar : Flatten<IAvatar>, setAvatar : SetStoreFunction<Flatten<IAvatar>>) : boolean {
  if (item.assign_type == "Occupational") {
    return false
  } else {
    const prefix = "detail.skills." + item.name + "."
    // @ts-ignore
    setAvatar(prefix + "assign_type", undefined)
    // @ts-ignore
    setAvatar(prefix + "era", undefined)
    // @ts-ignore
    setAvatar(prefix + "initial", undefined)
    // @ts-ignore
    setAvatar(prefix + "interest_skill_point", undefined)
    // @ts-ignore
    setAvatar(prefix + "occupation_skill_point", undefined)
    return true
  }
}
