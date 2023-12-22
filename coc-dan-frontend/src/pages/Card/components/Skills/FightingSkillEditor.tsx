import { For } from "solid-js"
import { getCardI18n } from "../../../../core/card/i18n/core"
import { SKILLS } from "../../../../core/card/resource"
import { useI18N } from "../../../../core/i18n"
import { genSkillI18n, getSkillI18nName } from "../../../../core/skill/i18n/core"
import { useAvatar } from "../../context"
import styles from "./SkillEditor.module.css"

export default () => {
  const { avatar, } = useAvatar()
  const i18n = useI18N()()
  const ts = genSkillI18n(i18n)
  const t = getCardI18n(i18n)
  const getFightingSkills = () : Map<string, number> => {
    const fightingSkills : Map<string, number> = new Map()
    for (const equipment of avatar.detail.equipments) {
      const item = equipment.item;
      if ("Weapon" in item) {
        const weapon = item.Weapon;
        const skill = SKILLS.get(weapon.skill_name)
        if (skill != undefined) {
          fightingSkills.set(skill.name, skill.initial)
        }
      }
    }
    for (const key in avatar.detail.skills) {
      const raw = avatar.detail.skills[key]
      const item = SKILLS.get(key)
      if (item != undefined) {
        if (item.category == "Fighting") {
          fightingSkills.set(key, raw.initial + raw.interest_skill_point + raw.occupation_skill_point)
        }
      }
    }
    if (!("Dodge" in fightingSkills)) {
      fightingSkills.set("Dodge", avatar.detail.characteristics.dex / 2)
    }
    return fightingSkills
  }
  return (
    <div style="margin-left : 2em">
      <p class="box-edit-header">{t("fightingSkillEditor.title")}</p>
      <div id="avatar-skill" class="box-shadow">
        <table style="width : 100%">
          <tbody>
            <For each={[...getFightingSkills()]}>
              {
                ([skill_name, skill_point], _i)  => {
                  return (
                    <tr>
                      <td class={`${styles.td}`}>
                        { getSkillI18nName(skill_name, ts) }
                      </td>
                      <td class={`${styles.td}`}>
                        { Math.floor(skill_point).toFixed(0) }
                      </td>
                    </tr>
                  )
                }
              }
            </For>
          </tbody>
        </table>
      </div>
    </div>
  )
}
