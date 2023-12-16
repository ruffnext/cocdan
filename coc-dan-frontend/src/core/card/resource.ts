import { IOccupation } from "../../bindings/avatar/IOccupation"
import { ISkill } from "../../bindings/avatar/ISkill"
import rawSkills from "./skills.json"
import rawOccupations from "./occupation.json"

const SKILLS : Map<string, ISkill> = new Map()
const skill_names = []
for (const item of rawSkills) {
  SKILLS.set(item.name, item as ISkill)
  skill_names.push(item.name)
}


const OCCUPATIONS : Map<string, IOccupation> = new Map()
for (const item of rawOccupations) {
  OCCUPATIONS.set(item.name, item as IOccupation)
}

export function getOccupationOrDefault(name : string) : IOccupation {
  const raw = OCCUPATIONS.get(name)
  if (raw != undefined) {
    return raw
  } else {
    return {
      name : "Custom",
      credit_rating : [0, 100],
      era : "None",
      attribute : ["edu"],
      occupational_skills : [],
      additional_skill_num : 2
    }
  }
}

export {SKILLS, OCCUPATIONS}
