import { For } from "solid-js"
import { IAvatar } from "../../../../bindings/IAvatar"
import { ISkillAssigned } from "../../../../bindings/avatar/ISkillAssigned"
import { useI18N } from "../../../../core/i18n"
import { useAvatar } from "../../context"
import * as flat from "flatten-type"
import { genSkillI18n } from "../../../../core/skill/i18n/core"
import { getCardI18n } from "../../../../core/card/i18n/core"
import { removeSkill, setSkill } from "./core"
import { getOccupationAvailableSkillCategories, remainingOccupationalSkillPoints } from "../../../../core/skill/core"
import SkillRowEditor from "./SkillRowEditor"
import { ISkillCategory } from "../../../../bindings/avatar/ISkillCategory"
import Dropdown, { IDropdownItem } from "../../../../components/Dropdown/Component"
import { SKILLS, SKILL_BY_CATEGORY } from "../../../../core/card/resource"
import styles from "./OccupationalSkillEditor.module.css"

function genAvailable(raw : flat.Flatten<IAvatar>) : Array<[ISkillCategory, number]> {
  const avatar : IAvatar = flat.unflatten(raw)
  const available : Map<ISkillCategory, number> = getOccupationAvailableSkillCategories(avatar.detail.occupation)

  for (const key in avatar.detail.skills) {
    const item = avatar.detail.skills[key]
    if (item.assign_type == "AdditionalOccupational") {
      const remain = available.get(item.category)
      available.set(item.category, (remain == undefined || remain <= 1) ? 0 : remain - 1)
    }
  }
  const res : Array<[ISkillCategory, number]> = []
  for (const [key, val] of available) {
    if (val > 0) {
      res.push([key, val])
    }
  }
  return res
}

export default () => {
  const { avatar, setAvatar } = useAvatar()
  const i18n = useI18N()()
  const ts = genSkillI18n(i18n)
  const t = getCardI18n(i18n)

  const raw : IAvatar = flat.unflatten(avatar)

  const AdditionalOccupationalSelector = (prop : {category : ISkillCategory, remain : number}) => {
    // @ts-ignore
    const categoryName : string = ts("category." + prop.category) || prop.category
    const categoryCandidates = SKILL_BY_CATEGORY.get(prop.category) || []
    const items : Array<IDropdownItem> = []
    for (const item of categoryCandidates) {
      items.push({
        // @ts-ignore
        label : ts("skill." + item.name + ".name") || item.name,
        value : item.name
      })
    }
    const text = t("additionalOccupationalSkillEditor.select", prop.remain, categoryName)

    const insertAdditionalOccupationalSkill = (val : string) : string => {
      const isExists = raw.detail.skills[val]
      if (isExists) {
        isExists.assign_type = "AdditionalOccupational"
        setSkill(isExists, avatar, setAvatar)
        return ""
      } else {
        const newSkill = SKILLS.get(val)
        if (newSkill == undefined) {
          return text
        }
        const newAssigned : ISkillAssigned = {
          name : newSkill.name,
          initial : newSkill.initial,
          era : newSkill.era,
          occupation_skill_point : 0,
          interest_skill_point : 0,
          category : newSkill.category,
          assign_type : "AdditionalOccupational"
        }
        setSkill(newAssigned, avatar, setAvatar)
        return text
      }
    }
    return (
      <tr style="height : 46px;">
        <td class={styles.td} colSpan={3} style="vertical-align : middle">
          <Dropdown items={items} initialLabel={text} setValue={insertAdditionalOccupationalSkill}/>
        </td>
      </tr>
    )
  }

  const getOccupationalSkills = (): Array<ISkillAssigned> => {
    const raw: IAvatar = flat.unflatten(avatar)
    const res: Array<ISkillAssigned> = []
    for (const key in raw.detail.skills) {
      const item = raw.detail.skills[key]
      if (item.assign_type == "Occupational" || item.assign_type == "AdditionalOccupational") {
        res.push(item)
      }
    }
    return res
  }

  const updateSkillPointEditor = (original: ISkillAssigned, modified: ISkillAssigned | undefined): string => {
    if (modified == undefined) {
      removeSkill(original, avatar, setAvatar)
    }
    if ((modified == undefined || (modified != undefined && setSkill(modified, avatar, setAvatar) == false))) {
      return (original.initial + original.interest_skill_point + original.occupation_skill_point).toFixed(0)
    } else {
      return (modified.initial + modified.interest_skill_point + modified.occupation_skill_point).toFixed(0)
    }
  }

  return (
    <div>
      <p class="box-edit-header">{t("occupationalSkillEditor.title") + t("occupationalSkillEditor.remain", remainingOccupationalSkillPoints(flat.unflatten(avatar)))}</p>
      <div id="avatar-skill" class="box-shadow">
        <table>
          <tbody>
            <For each={getOccupationalSkills()}>{
              (item, _i) => <SkillRowEditor removable={item.assign_type == "AdditionalOccupational"} item={item} updateSkillPointEditor={updateSkillPointEditor} translator={ts} />
            }</For>
            <For each={genAvailable(avatar)}>{
              ([category, remain], _i) => <AdditionalOccupationalSelector category={category} remain={remain} />
            }</For>
          </tbody>
        </table>
      </div>
    </div>
  )
}
