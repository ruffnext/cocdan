import { For } from "solid-js"
import { IAvatar } from "../../../../bindings/IAvatar"
import { ISkillAssigned } from "../../../../bindings/avatar/ISkillAssigned"
import { useI18N } from "../../../../core/i18n"
import { useAvatar } from "../../context"
import * as flat from "flatten-type"
import { genSkillI18n } from "../../../../core/skill/i18n/core"
import { getCardI18n } from "../../../../core/card/i18n/core"
import { removeSkill, setSkill } from "./core"
import { remainingOccupationalSkillPoints } from "../../../../core/skill/core"
import SkillRowEditor from "./SkillRowEditor"


export default () => {
  const { avatar, setAvatar } = useAvatar()
  const i18n = useI18N()()
  const ts = genSkillI18n(i18n)
  const t = getCardI18n(i18n)

  const getOccupationalSkills = (): Array<ISkillAssigned> => {
    const raw: IAvatar = flat.unflatten(avatar)
    const res: Array<ISkillAssigned> = []
    for (const key in raw.detail.skills) {
      const item = raw.detail.skills[key]
      if (item.assign_type == "AdditionalOccupational" || item.assign_type == "Occupational") {
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
              (item, _i) => <SkillRowEditor item={item} updateSkillPointEditor={updateSkillPointEditor} translator={ts} />
            }</For>
          </tbody>
        </table>
      </div>
    </div>
  )
}
