import { For, createSignal } from "solid-js"
import { IAvatar } from "../../../../bindings/IAvatar"
import { ISkillAssigned } from "../../../../bindings/avatar/ISkillAssigned"
import { useI18N } from "../../../../core/i18n"
import { useAvatar } from "../../context"
import * as flat from "flatten-type"
import { genSkillI18n } from "../../../../core/skill/i18n/core"
import styles from "./OccupationalSkillEditor.module.css"
import { maximumOccupationalSkillPoint } from "../../../../core/card/calc"
import { deepClone } from "../../../../core/utils"
import { getCardI18n } from "../../../../core/card/i18n/core"

interface ISkillPointEditor {
  item : ISkillAssigned,
  updateItem : (original : ISkillAssigned, modified : ISkillAssigned) => string
}

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

  const getSkillName = (name: string): string => {
    // @ts-ignore
    const res = ts(name + ".name")
    if (res == undefined) {
      return name
    } else {
      // @ts-ignore
      return res
    }
  }

  const remainingSkillPoints = (val : IAvatar | undefined): number => {
    const raw: IAvatar = val || flat.unflatten(avatar)
    const maximum = maximumOccupationalSkillPoint(raw.detail.occupation, raw.detail.attrs)
    var res = 0
    for (const key in raw.detail.skills) {
      const item = raw.detail.skills[key]
      res += item.occupation_skill_point
    }
    return maximum - res
  }

  const increaseSkill = (item: ISkillAssigned) => {
    const val = deepClone(item)
    val.occupation_skill_point += 1
    setSkill(val)
  }

  const decreaseSkill = (item: ISkillAssigned) => {
    const val = deepClone(item)
    val.occupation_skill_point -= 1
    setSkill(val)
  }

  const setSkill = (item : ISkillAssigned) : boolean => {
    const total = item.occupation_skill_point + item.initial + item.interest_skill_point
    if (total < 0) return false
    if (total > 99) return false
    const res = item.occupation_skill_point
      // @ts-ignore
    if (item.name == "Credit Rating" && total < avatar["detail.occupation.credit_rating.0"]) return false
      // @ts-ignore
    if (item.name == "Credit Rating" && total > avatar["detail.occupation.credit_rating.1"]) return false
    if (res < 0) return false

    const raw : IAvatar = flat.unflatten(avatar)
    var found = false
    for (const key in raw.detail.skills) {
      const val = raw.detail.skills[key]
      if (val.name == item.name) {
        raw.detail.skills[key].occupation_skill_point = res
        found = true
        break
      }
    }
    if (found == false) {
      raw.detail.skills[item.name] = item
    }

    // @ts-ignore
    setAvatar("detail.skills." + item.name + ".occupation_skill_point", res)
    if (!found) {
      // @ts-ignore
      setAvatar("detail.skills." + item.name + ".name", item.name)
      // @ts-ignore
      setAvatar("detail.skills." + item.name + ".initial", item.initial)
      // @ts-ignore
      setAvatar("detail.skills." + item.name + ".era", item.era)
      // @ts-ignore
      setAvatar("detail.skills." + item.name + ".interest_skill_point", item.interest_skill_point)
      // @ts-ignore
      setAvatar("detail.skills." + item.name + ".assign_type", item.assign_type)
    }
    return true
  }

  const updateSkillPointEditor = (original : ISkillAssigned, modified : ISkillAssigned) : string => {
    if (setSkill(modified)) {
      return (modified.initial + modified.interest_skill_point + modified.occupation_skill_point).toFixed(0)
    } else {
      return (original.initial + original.interest_skill_point + original.occupation_skill_point).toFixed(0)
    }
  }

  const SkillPointEditor = (prop : ISkillPointEditor) => {
    const [val, setVal] = createSignal((prop.item.initial + prop.item.interest_skill_point + prop.item.occupation_skill_point).toFixed(0))
    const submit = () => {
      var fp = parseInt(val())
      const initialStr = (prop.item.initial + prop.item.occupation_skill_point + prop.item.interest_skill_point).toFixed(0)
      if (isNaN(fp) || fp < 0 || fp >= 100) {
        setVal(initialStr)
      }
      fp = fp - prop.item.interest_skill_point - prop.item.initial
      const modified = deepClone(prop.item)
      modified.occupation_skill_point = fp
      const res = prop.updateItem(prop.item, modified)
      setVal(res)
    }
    return (
      <input 
        class={styles.skill_point_input}
        value={val()} 
        onChange={(e) => setVal(e.target.value)} 
        onBlur={() => submit()}
      />
    )
  }

  function editorRow(item: ISkillAssigned) {
    return (
      <tr>
        <td class={styles.td}>{getSkillName(item.name)}</td>
        <td class={`${styles.td} ${styles.skill_point_input}`}>
          <SkillPointEditor item={item} updateItem={updateSkillPointEditor} />
        </td>
        <td class={styles.button_container}>
          <p class={`${styles.button_item} ${styles.button_item_plus}`} onClick={() => increaseSkill(item)}>+</p>
          <p class={`${styles.button_item} ${styles.button_item_minus}`} onClick={() => decreaseSkill(item)}>-</p>
        </td>
      </tr>
    )
  }

  return (
    <>
      <table>
        <tbody>
          <For each={getOccupationalSkills()}>{
            (item, _i) => editorRow(item)
          }</For>
        </tbody>
      </table>
      <div>{t("occupationalSkillPoint.remain") + remainingSkillPoints(undefined) }</div>
    </>
  )
}
