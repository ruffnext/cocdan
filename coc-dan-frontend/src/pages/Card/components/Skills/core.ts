import { IAvatar } from "../../../../bindings/IAvatar"
import { ISkillAssigned } from "../../../../bindings/avatar/ISkillAssigned"
import { SetStoreFunction } from "solid-js/store"
import { genAvatarAvailableOptionalOccupationSkills } from "../../../../core/skill/core"



export function setSkill(item: ISkillAssigned, avatar: IAvatar, setAvatar: SetStoreFunction<IAvatar>): boolean {
  const total = item.occupation_skill_point + item.initial + item.interest_skill_point
  if (total < 0) return false
  if (total > 99) return false
  const res = item.occupation_skill_point
  if (item.name == "Credit Rating" && total < avatar.detail.occupation.credit_rating[0]) return false
  if (item.name == "Credit Rating" && total > avatar.detail.occupation.credit_rating[1]) return false
  if (res < 0) return false

  for (const key in avatar.detail.skills) {
    const val = avatar.detail.skills[key]
    if (val.name == item.name) {
      setAvatar("detail", "skills", key, "occupation_skill_point", res)
      return true
    }
  }

  // assign a new occupational skill
  if (item.assign_type == "AdditionalOccupational") {

    // initialize total available skill slot
    const available = genAvatarAvailableOptionalOccupationSkills(avatar)
    const thisClass = available.get(item.category)
    if (thisClass == undefined) {
      return false
    }
    const [candidates, remain] = thisClass
    var foundInner = false
    for (const candidate of candidates) {
      if (candidate.name == item.name) {
        foundInner = true
        break
      }
    }
    if (foundInner == false && item.category != "Any") {
      return false
    }

    if (remain <= 0) {
      const anyRemain = available.get("Any")
      if (anyRemain == undefined || anyRemain[1] <= 0) {
        return false
      } else {
        item.category = "Any"
      }
    }
  }

  setAvatar("detail", "skills", item.name, item)
  return true
}

export function removeSkill(item: ISkillAssigned, _avatar: IAvatar, setAvatar: SetStoreFunction<IAvatar>): boolean {
  if (item.assign_type == "Occupational") {
    return false
  } else {
    if (item.assign_type == "AdditionalOccupational" && item.interest_skill_point != 0) {
      setAvatar("detail", "skills", item.name, "assign_type", "Interest")
    } else {
      // @ts-ignore
      setAvatar("detail", "skills", item.name, undefined)
    }
    return true
  }
}
