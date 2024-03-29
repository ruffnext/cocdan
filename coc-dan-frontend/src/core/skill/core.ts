import { IAvatar } from "../../bindings/IAvatar"
import { ICharacteristics } from "../../bindings/avatar/ICharacteristics"
import { IOccupation } from "../../bindings/avatar/IOccupation"
import { IOptionalOccupationalSkill } from "../../bindings/avatar/IOptionalOccupationalSkill"
import { ISkill } from "../../bindings/avatar/ISkill"
import { ISkillAssigned } from "../../bindings/avatar/ISkillAssigned"
import { ISkillCategory } from "../../bindings/avatar/ISkillCategory"
import { SKILLS, SKILL_BY_CATEGORY } from "../card/resource"
import { deepClone } from "../utils"
import { ISkillAssignType } from "./def"

export function maximumOccupationalSkillPoint(
  occupation: IOccupation,
  characteristics: ICharacteristics
): number {
  if (occupation.characteristics.length == 1) {
    return characteristics[occupation.characteristics[0]] * 4
  } else if (occupation.characteristics.length >= 2) {
    const attrValues: Array<number> = []
    for (const attr of occupation.characteristics) {
      attrValues.push(characteristics[attr])
    }
    const sorted = attrValues.sort().reverse()
    return sorted[0] * 2 + sorted[1] * 2
  }
  return 0
}

export function remainingOccupationalSkillPoints(raw: IAvatar): number {
  const maximum = maximumOccupationalSkillPoint(raw.detail.occupation, raw.detail.characteristics)
  var res = 0
  for (const key in raw.detail.skills) {
    const item = raw.detail.skills[key]
    res += item.occupation_skill_point
  }
  return maximum - res
}

export function remainInterestSkillPoints(raw : IAvatar) : number {
  var res = raw.detail.characteristics.int * 2
  for (const key in raw.detail.skills) {
    const item = raw.detail.skills[key]
    res -= item.interest_skill_point
  }
  return res
}

export function genAvatarAvailableOptionalOccupationSkills(avatar : IAvatar) : Map<ISkillCategory, [Array<ISkill>, number]> {
  const available : Array<IOptionalOccupationalSkill> = getOccupationAvailableSkillCategories(deepClone(avatar.detail.occupation))
  const selected : Map<string, ISkillAssigned> = new Map()

  // setup additional selected
  for (const key in avatar.detail.skills) {
    const item = avatar.detail.skills[key]
    selected.set(item.name, item)
  }
  const resRaw : Map<ISkillCategory, [Array<ISkill>, number]> = new Map()

  // handle category specified skills
  for (const item of available) {
    var remainNum = item.limit
    var remainSelectable : Array<ISkill> = []

    if (item.candidates.length == 0) {
      if (item.category != "Any") {
        remainSelectable = SKILL_BY_CATEGORY.get(item.category) || []
      } else {
        for (const [_name, val] of SKILLS) {
          remainSelectable.push(deepClone(val))
        }
      }
    } else {
      for (const key of item.candidates) {
        const skill = SKILLS.get(key)
        if (skill == undefined) continue
        remainSelectable.push(deepClone(skill))
      }
    }

    // remove selected
    if (item.category == "Any") {
      resRaw.set(item.category, [remainSelectable, remainNum])
      continue
    }
    const newCandidates : Array<ISkill> = []
    for (const remain of remainSelectable) {
      const val = selected.get(remain.name)
      if (val != undefined && (val.category == item.category && val.assign_type & ISkillAssignType.Occupational)) {
        remainNum -= 1
      } else {
        newCandidates.push(remain)
        continue

      }
    }

    if (remainNum > 0 && newCandidates.length > 0) {
      resRaw.set(item.category, [newCandidates, remainNum])
    }
  }

  const anyRes = resRaw.get("Any")
  if (anyRes != undefined) {
    var remains = anyRes[1]
    const newCandidates : ISkill[] = []
    for (const item of anyRes[0]) {
      const selectedItem = selected.get(item.name)
      if (selectedItem != undefined) {
        if ((selectedItem.assign_type & ISkillAssignType.Optional) != 0
           && (selectedItem.assign_type & ISkillAssignType.Occupational) != 0
           && selectedItem.category == "Any") {
          remains -= 1
        }
        continue
      }
      newCandidates.push(item)
    }
    resRaw.set("Any", [newCandidates, Math.max(remains, 0)])
  }

  return resRaw
}

export function getOccupationAvailableSkillCategories(occupation: IOccupation) : Array<IOptionalOccupationalSkill> {
  const res : Array<IOptionalOccupationalSkill> = []
  for (const item of occupation.occupational_skills) {
    if (typeof item == "string") {
      continue
    }
    res.push(item)
  }
  return res
}


