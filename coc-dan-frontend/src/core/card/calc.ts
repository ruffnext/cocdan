import { IAttrs } from "../../bindings/avatar/IAttrs"
import { IOccupation } from "../../bindings/avatar/IOccupation"

export function maxHP (con : number, siz : number) : number {
  return Math.floor((con + siz) / 10)
}

export function maxSan (pow : number) : number {
  return Math.floor(pow)
}

export function maxMP (pow : number) : number {
  return Math.floor(pow / 5)
}

export function maximumOccupationalSkillPoint (
  occupation : IOccupation,
  attrs : IAttrs
) : number {
  if (occupation.attribute.length == 1) {
    return attrs[occupation.attribute[0]] * 4
  } else if (occupation.attribute.length >= 2) {
    const attrValues : Array<number> = []
    for (const attr of occupation.attribute) {
      attrValues.push(attrs[attr])
    }
    const sorted = attrValues.sort().reverse()
    return sorted[0] * 2 + sorted[1] * 2
  }
  return 0
}