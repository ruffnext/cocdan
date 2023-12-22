import { For, Show } from "solid-js"
import { IAvatar } from "../../../../bindings/IAvatar"
import { ISkillAssigned } from "../../../../bindings/avatar/ISkillAssigned"
import { useI18N } from "../../../../core/i18n"
import { useAvatar } from "../../context"
import { genSkillI18n } from "../../../../core/skill/i18n/core"
import { getCardI18n } from "../../../../core/card/i18n/core"
import { removeSkill, setSkill } from "./core"
import { genAvatarAvailableOptionalOccupationSkills, remainingOccupationalSkillPoints } from "../../../../core/skill/core"
import SkillRowEditor from "./SkillRowEditor"
import { ISkillCategory } from "../../../../bindings/avatar/ISkillCategory"
import Dropdown, { IDropdownItem } from "../../../../components/Dropdown/Component"
import { SKILLS } from "../../../../core/card/resource"
import styles from "./SkillEditor.module.css"
import { ISkill } from "../../../../bindings/avatar/ISkill"
import { resetSkill as resetOccupationalSkill } from "../utils"
import { deepClone } from "../../../../core/utils"
import { ISkillAssignType } from "../../../../core/skill/def"



export default () => {
  const { avatar, setAvatar } = useAvatar()
  const i18n = useI18N()()
  const ts = genSkillI18n(i18n)
  const t = getCardI18n(i18n)
  

  const AdditionalOccupationalSelector = (prop : {category : ISkillCategory, candidates : Array<ISkill>, remain : number}) => {
    const items : Array<IDropdownItem> = []
    for (const item of prop.candidates) {
      items.push({
        // @ts-ignore
        label : ts("skill." + item.name + ".name") || item.name,
        value : item.name
      })
    }
    // @ts-ignore
    const text = t("additionalOccupationalSkillEditor.select", prop.remain, ts("category." + prop.category))
    const insertAdditionalOccupationalSkill = (val : string) : string => {
      if (val in avatar.detail.skills) {
        const isExists = deepClone(avatar.detail.skills[val])
        if (isExists.assign_type & ISkillAssignType.Occupational) {
          return text
        }
        isExists.assign_type = isExists.assign_type | (ISkillAssignType.Optional | ISkillAssignType.Occupational)
        setSkill(isExists, avatar, setAvatar)
        return ""
      } else {
        const newSkill = deepClone(SKILLS.get(val))
        if (newSkill == undefined) {
          return text
        }
        const newAssigned : ISkillAssigned = {
          name : newSkill.name,
          initial : newSkill.initial,
          era : newSkill.era,
          occupation_skill_point : 0,
          interest_skill_point : 0,
          category : prop.category,
          assign_type : (ISkillAssignType.Optional | ISkillAssignType.Occupational)
        }
        setSkill(newAssigned, avatar, setAvatar)
        return text
      }
    }
    return (
      <tr style="height : 46px;">
        <td class={`${styles.td} ${styles.tr_clickable}`} colSpan={3} style="vertical-align : middle; padding : 0; height : 46px;">
          <Dropdown items={items} initialLabel={text} setValue={insertAdditionalOccupationalSkill}/>
        </td>
      </tr>
    )
  }

  const getOccupationalSkills = (): Array<ISkillAssigned> => {
    const res: Array<ISkillAssigned> = []
    for (const key in avatar.detail.skills) {
      const item = avatar.detail.skills[key]
      if (item.assign_type & ISkillAssignType.Occupational) {
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

  const genCandidate = (val : IAvatar) : Array<[ISkillCategory, ISkill[], number]> => {
    const raw = genAvatarAvailableOptionalOccupationSkills(val)
    const res : Array<[ISkillCategory, ISkill[], number]> = []
    for (const [category, [candidates, remain]] of raw) {
      res.push([category, candidates, remain])
    }
    return res
  }

  return (
    <div>
      <p class="box-edit-header">{t("occupationalSkillEditor.title") + t("occupationalSkillEditor.remain", remainingOccupationalSkillPoints(avatar))}</p>
      <div id="avatar-skill" class="box-shadow">
        <table style="width : 100%">
          <tbody>
            <For each={getOccupationalSkills()}>{
              (item, _i) => <SkillRowEditor assignType={ISkillAssignType.Occupational} removable={(item.assign_type & ISkillAssignType.Optional) != 0} item={item} updateSkillPointEditor={updateSkillPointEditor} translator={ts} />
            }</For>
            <For each={genCandidate(avatar)}>{
              ([category, candidates, remain], _i) => <Show when={remain > 0}>
                <AdditionalOccupationalSelector candidates={candidates} category={category} remain={remain} />
              </Show>
            }</For>
            <tr>
              <td class={`${styles.td} ${styles.tr_clickable_danger}`} 
                  colSpan={3} style="vertical-align : middle"
                  onclick={() => resetOccupationalSkill(avatar, setAvatar)}>
                {t("occupationalSkillEditor.reset")}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  )
}
