import { IAvatar } from "../../../bindings/IAvatar"
import { ISkillAssigned } from "../../../bindings/avatar/ISkillAssigned"
import { SetStoreFunction } from "solid-js/store"
import { deepClone } from "../../../core/utils"
import { SKILLS } from "../../../core/card/resource"
import { ISkillAssignType } from "../../../core/skill/def"

export function resetSkill(raw: IAvatar, setAvatar: SetStoreFunction<IAvatar>) {
  var modifies : Record<string, ISkillAssigned | undefined> = {}
  const original = deepClone(raw.detail.skills)
  const setSkill = (skill: ISkillAssigned) => {
    if (skill.name == "Language") {
      skill.initial = raw.detail.characteristics.edu
    }
    modifies[skill.name] = skill
  }

  for (const key in original) {
    const item = original[key]
    item.occupation_skill_point = 0
    setSkill(item)
    if (item.assign_type == (ISkillAssignType.Occupational | ISkillAssignType.Optional)) {
      modifies[key] = undefined
    } else if (item.assign_type == (ISkillAssignType.Occupational | ISkillAssignType.Optional | ISkillAssignType.Interest)) {
      item.assign_type = ISkillAssignType.Interest
      setSkill(item)
    }
  }

  // @ts-ignore  reset Credit Rating
  const credit_rating: ISkill = deepClone(SKILLS.get("Credit Rating"))
  setSkill({
    name: credit_rating.name,
    initial: 0,
    era: "None",
    occupation_skill_point: raw.detail.occupation.credit_rating[0],
    interest_skill_point: 0,
    category: "Any",
    assign_type: ISkillAssignType.Occupational
  })
  setAvatar("detail", "skills", modifies)
}
