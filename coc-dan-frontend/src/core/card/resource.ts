import { IOccupation } from "../../bindings/avatar/IOccupation"
import { ISkill } from "../../bindings/avatar/ISkill"
import rawSkills from "./skills.json"
import rawOccupations from "./occupation.json"
import { ISkillAssigned } from "../../bindings/avatar/ISkillAssigned"

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

export function initOccupationalSkill(occupation : IOccupation) : Record<string, ISkillAssigned> {
  const res : Record<string, ISkillAssigned> = {}
  for (const skill of occupation.occupational_skills) {
    const item = SKILLS.get(skill)
    if (item != undefined) {
      res[item.name] = {
        name : item.name,
        era : item.era,
        initial : item.initial,
        occupation_skill_point : 0,
        interest_skill_point : 0,
        assign_type : "Occupational"
      }
    }
  }

  const creditRating = SKILLS.get("Credit Rating")
  if (creditRating == undefined) {
    throw Error("Credit Rating not found")
  }
  
  res[creditRating.name] = {
    name : creditRating.name,
    era : creditRating.era,
    initial : creditRating.initial,
    occupation_skill_point : occupation.credit_rating[0],
    interest_skill_point : 0,
    assign_type : "Occupational"
  }

  return res
}

export {SKILLS, OCCUPATIONS}
