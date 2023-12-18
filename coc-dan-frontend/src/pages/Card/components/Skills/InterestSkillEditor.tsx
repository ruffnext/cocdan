import { For } from "solid-js"
import { getCardI18n } from "../../../../core/card/i18n/core"
import { useI18N } from "../../../../core/i18n"
import { remainInterestSkillPoints } from "../../../../core/skill/core"
import { genSkillI18n } from "../../../../core/skill/i18n/core"
import { useAvatar } from "../../context"
import styles from "./SkillEditor.module.css"
import SkillRowEditor from "./SkillRowEditor"
import { ISkillAssigned } from "../../../../bindings/avatar/ISkillAssigned"
import { removeSkill, setSkill } from "./core"
import { deepClone } from "../../../../core/utils"
import { SKILLS } from "../../../../core/card/resource"
import { IDropdownItem } from "../../../../components/Dropdown/Component"
import Dropdown from "../../../../components/Dropdown/Component"

export default () => {
  const { avatar, setAvatar } = useAvatar()
  const i18n = useI18N()()
  const ts = genSkillI18n(i18n)
  const t = getCardI18n(i18n)
  const buttonLabel = t("interestSkillEditor.add")

  const getInterestSkills = () : Array<ISkillAssigned> => {
    const res : ISkillAssigned[] = []
    for (const key in avatar.detail.skills) {
      const item = avatar.detail.skills[key]
      if (item.assign_type == "Interest") {
        res.push(deepClone(item))
      }
    }
    return res
  }

  const getAvailableInterestSkills = () : Array<IDropdownItem> => {
    const res : Array<IDropdownItem> = []
    const selected : Set<string> = new Set()
    for (const key in avatar.detail.skills) {
      selected.add(key)
    }
    for (const [_key, val] of SKILLS) {
      if (selected.has(val.name)) continue
      res.push({
        // @ts-ignore
        label : ts("skill." + val.name + ".name") || val.name,
        value : val.name
      })
    }
    return res
  }

  const updateSkillPointEditor = (original : ISkillAssigned, modified : ISkillAssigned | undefined) : string => {
    if (modified == undefined) {
      removeSkill(original, avatar, setAvatar)
    }
    if ((modified == undefined || (modified != undefined && setSkill(modified, avatar, setAvatar) == false))) {
      return (original.initial + original.interest_skill_point + original.occupation_skill_point).toFixed(0)
    } else {
      return (modified.initial + modified.interest_skill_point + modified.occupation_skill_point).toFixed(0)
    }
  }

  const setInterestSkill = (skillName : string) : string => {
    const raw = SKILLS.get(skillName)
    if (raw == undefined) return buttonLabel
    setSkill({
      name : raw.name,
      initial : raw.initial,
      era : raw.era,
      occupation_skill_point : 0,
      interest_skill_point : 0,
      category : raw.category,
      assign_type : "Interest"
    }, avatar, setAvatar)
    return buttonLabel
  }


  return (
    <div style="margin-left : 2em">
      <p class="box-edit-header">{t("interestSkillEditor.title") + t("interestSkillEditor.remain", remainInterestSkillPoints(avatar))}</p>
      <div id="avatar-skill" class="box-shadow">
        <table style="width : 100%">
          <tbody>
            <For each={getInterestSkills()}>{
              (item, _i) => <SkillRowEditor removable={true} item={item} updateSkillPointEditor={updateSkillPointEditor} translator={ts} />
            }</For>
            <tr>
              <td class={`${styles.td} ${styles.tr_clickable}`} 
                  colSpan="3" style="vertical-align : middle; width : 100%"
                  onclick={() => {}}>
                <Dropdown items={getAvailableInterestSkills()} initialLabel={buttonLabel} setValue={setInterestSkill} />
              </td>
            </tr>
            <tr>
              <td class={`${styles.td} ${styles.tr_clickable_danger}`} 
                  colSpan="3" style="vertical-align : middle"
                  onclick={() => {}}>
                {t("interestSkillEditor.reset")}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  )
}