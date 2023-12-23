import { IAvatar } from "../../../../bindings/IAvatar"
import { ISkillAssigned } from "../../../../bindings/avatar/ISkillAssigned"
import { SetStoreFunction } from "solid-js/store"
import { genAvatarAvailableOptionalOccupationSkills } from "../../../../core/skill/core"
import { ISkillAssignType } from "../../../../core/skill/def"



export function setSkill(item: ISkillAssigned, avatar: IAvatar, setAvatar: SetStoreFunction<IAvatar>): boolean {
  const total = item.occupation_skill_point + item.initial + item.interest_skill_point
  if (total < 0) return false
  if (total > 99) return false
  if (item.name == "CreditRating" && total < avatar.detail.occupation.credit_rating[0]) return false
  if (item.name == "CreditRating" && total > avatar.detail.occupation.credit_rating[1]) return false
  if (item.interest_skill_point < 0 || item.occupation_skill_point < 0) return false

  for (const key in avatar.detail.skills) {
    const val = avatar.detail.skills[key]
    // if modify existing skill
    if (val.name == item.name) {
      setAvatar("detail", "skills", key, item)
      return true
    }
  }

  // assign a new occupational skill, check is it available
  if (item.assign_type == (ISkillAssignType.Optional | ISkillAssignType.Occupational)) {

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
  if (item.assign_type == ISkillAssignType.Occupational) {
    return false
  } else {
    if (item.assign_type & ISkillAssignType.Optional && item.assign_type & ISkillAssignType.Interest) {
      setAvatar("detail", "skills", item.name, "assign_type", ISkillAssignType.Interest)
    } else {
      // @ts-ignore
      setAvatar("detail", "skills", item.name, undefined)
    }
    return true
  }
}
