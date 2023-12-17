import { createEffect, createSignal } from "solid-js"
import { ISkillAssigned } from "../../../../bindings/avatar/ISkillAssigned"
import { ISkillTranslator, getSkillI18nName } from "../../../../core/skill/i18n/core"
import styles from "./OccupationalSkillEditor.module.css"
import { deepClone } from "../../../../core/utils"

interface Props {
  item : ISkillAssigned,
  translator : ISkillTranslator,
  updateSkillPointEditor : (original : ISkillAssigned, modified : ISkillAssigned | undefined) => string,
  removable? : boolean
}

interface ISkillPointEditor {
  item : ISkillAssigned,
  updateItem : (original : ISkillAssigned, modified : ISkillAssigned) => string
}

export default (prop : Props) => {
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
    createEffect(() => {
      setVal((prop.item.initial + prop.item.occupation_skill_point + prop.item.interest_skill_point).toFixed(0))
    })
    
    return (
      <input 
        class={styles.skill_point_input}
        value={val()} 
        onChange={(e) => setVal(e.target.value)} 
        onBlur={() => submit()}
      />
    )
  }

  const increaseSkill = () => {
    const val = deepClone(prop.item)
    val.occupation_skill_point += 1
    prop.updateSkillPointEditor(prop.item, val)
  }

  const decreaseSkill = () => {
    const val = deepClone(prop.item)
    val.occupation_skill_point -= 1
    prop.updateSkillPointEditor(prop.item, val)
  }

  return (
    <tr>
      <td style={"vertical-align: middle;"} 
        class={`${styles.td} ${(prop.removable == true) ? styles.removable : ""}`} onClick={() => prop.updateSkillPointEditor(prop.item, undefined)}>
        {getSkillI18nName(prop.item.name, prop.translator)}
      </td>
      <td class={`${styles.td} ${styles.no_padding} ${styles.skill_point_input}`}>
        <SkillPointEditor item={prop.item} updateItem={prop.updateSkillPointEditor} />
      </td>
      <td class={styles.button_container}>
        <p class={`${styles.button_item} ${styles.button_item_plus}`} onClick={() => increaseSkill()}>+</p>
        <p class={`${styles.button_item} ${styles.button_item_minus}`} onClick={() => decreaseSkill()}>-</p>
      </td>
    </tr>
  )
}
