import { IOccupation } from "../../bindings/avatar/IOccupation"
import { ISkill } from "../../bindings/avatar/ISkill"
import rawSkills from "./skills.json"
import rawOccupations from "./occupation.json"
import { ISkillAssigned } from "../../bindings/avatar/ISkillAssigned"
import { ISkillCategory } from "../../bindings/avatar/ISkillCategory"

const SKILLS : Map<string, ISkill> = new Map()
const SKILL_BY_CATEGORY : Map<ISkillCategory, ISkill[]> = new Map()
SKILL_BY_CATEGORY.set("Any", [])

const skill_names = []
for (const val of rawSkills) {
  const item = val as ISkill
  SKILLS.set(item.name, item as ISkill)
  skill_names.push(item.name)
  if (! (SKILL_BY_CATEGORY.has(item.category))) {
    SKILL_BY_CATEGORY.set(item.category, [])
  }
  // @ts-ignore
  SKILL_BY_CATEGORY.get(item.category).push(item)
  // @ts-ignore
  SKILL_BY_CATEGORY.get("Any").push(item)
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
      additional_skills : [
        "Any",
        "Any"
      ]
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
        assign_type : "Occupational",
        category : "Any"
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
    assign_type : "Occupational",
    category : "Any"
  }

  return res
}

export {SKILLS, OCCUPATIONS, SKILL_BY_CATEGORY}
