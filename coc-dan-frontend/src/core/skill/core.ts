import { IAvatar } from "../../bindings/IAvatar"
import { IAttrs } from "../../bindings/avatar/IAttrs"
import { IOccupation } from "../../bindings/avatar/IOccupation"
import { ISkillCategory } from "../../bindings/avatar/ISkillCategory"

export function maximumOccupationalSkillPoint(
  occupation: IOccupation,
  attrs: IAttrs
): number {
  if (occupation.attribute.length == 1) {
    return attrs[occupation.attribute[0]] * 4
  } else if (occupation.attribute.length >= 2) {
    const attrValues: Array<number> = []
    for (const attr of occupation.attribute) {
      attrValues.push(attrs[attr])
    }
    const sorted = attrValues.sort().reverse()
    return sorted[0] * 2 + sorted[1] * 2
  }
  return 0
}

export function remainingOccupationalSkillPoints(raw: IAvatar): number {
  const maximum = maximumOccupationalSkillPoint(raw.detail.occupation, raw.detail.attrs)
  var res = 0
  for (const key in raw.detail.skills) {
    const item = raw.detail.skills[key]
    res += item.occupation_skill_point
  }
  return maximum - res
}

export function getOccupationAvailableSkillCategories(occupation: IOccupation): Map<ISkillCategory, number> {
  const available: Map<ISkillCategory, number> = new Map()
  for (const item of occupation.additional_skills) {
    const res = available.get(item)
    if (res == undefined) {
      available.set(item, 1)
    } else {
      available.set(item, res + 1)
    }
  }
  return available
}


