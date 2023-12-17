import { IAvatar } from "../../../bindings/IAvatar"
import { ISkillAssigned } from "../../../bindings/avatar/ISkillAssigned"
import { SetStoreFunction } from "solid-js/store"
import { deepClone } from "../../../core/utils"
import { SKILLS } from "../../../core/card/resource"

export function resetSkill(raw: IAvatar, setAvatar: SetStoreFunction<IAvatar>) {
  var modifies : Record<string, ISkillAssigned | undefined> = {}
  const setSkill = (skill: ISkillAssigned) => {
    if (skill.name == "Language") {
      skill.initial = raw.detail.attrs.edu
    }
    modifies[skill.name] = skill
  }

  // remove all skills
  for (const key in raw.detail.skills) {
    modifies[key] = undefined
  }

  // restore interest skills
  for (const key in raw.detail.skills) {
    const item = raw.detail.skills[key]
    if (item.assign_type == "Interest") {
      setSkill(item)
    }
  }

  // set occupational skills
  const names: Array<string> = []
  for (const item of raw.detail.occupation.occupational_skills) {
    if (typeof item == "string") {
      names.push(item)
    }
  }
  for (const name of names) {
    var assigned: ISkillAssigned | null = null
    if (name in raw.detail.skills) {
      assigned = deepClone(raw.detail.skills[name])
      assigned.occupation_skill_point = 0
    } else {
      const item = deepClone(SKILLS.get(name))
      if (item == undefined) { continue }
      assigned = {
        name: item.name,
        initial: item.initial,
        era: item.era,
        occupation_skill_point: 0,
        interest_skill_point: 0,
        category: item.category,
        assign_type: "Occupational"
      }
    }
    setSkill(assigned)
  }
  // @ts-ignore
  const credit_rating: ISkill = deepClone(SKILLS.get("Credit Rating"))
  setSkill({
    name: credit_rating.name,
    initial: 0,
    era: "None",
    occupation_skill_point: raw.detail.occupation.credit_rating[0],
    interest_skill_point: 0,
    category: "Any",
    assign_type: "Occupational"
  })
  setAvatar("detail", "skills", modifies)
}