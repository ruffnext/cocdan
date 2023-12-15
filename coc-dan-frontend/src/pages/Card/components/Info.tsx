import { useI18N } from "../../../core/i18n"
import { useAvatar } from "../context"
import { getCardI18n } from "../i18n/core"
import CellInput from "./CellInput"
import "./table.css"

export default () => {
  const { avatar, setAvatar } = useAvatar()  
  const t = getCardI18n(useI18N()())

  const setName = (val: string): string => {
    return val
  }

  const setGender = (val: string): string => {
    return val
  }

  const setCareer = (val : string) : string => {
    return val
  }

  const setAge = (val : string) : string => {
    const newVal = parseInt(val)
    if (isNaN(newVal) || newVal < 0) {
      return avatar["detail.descriptor.age"].toFixed(0)
    }
    setAvatar("detail.descriptor.age", newVal)
    return newVal.toFixed(0)
  }

  const setHomeland = (val : string) : string => {
    return val
  }
  
  return (
    <>
      <p class="box-edit-header">{t("cardEditor.investor.title")}</p>
      <table>
        <tbody>
          <tr>
            <td>{t("cardEditor.investor.name")}</td>
            <td colSpan="3"><CellInput value={avatar.name} setValue={setName} /></td>
          </tr>
          <tr>
            <td style="width : 17%">{t("cardEditor.investor.gender")}</td>
            <td style="width : 33%"><CellInput value={avatar["detail.descriptor.gender"]} setValue={setGender} /></td>
            <td style="width : 17%">{t("cardEditor.investor.age")}</td>
            <td style="width : 33%"><CellInput value={avatar["detail.descriptor.age"].toFixed(0)} setValue={setAge} /></td>
          </tr>
          <tr>
            <td>{t("cardEditor.investor.career")}</td>
            <td><CellInput value={avatar["detail.descriptor.career"]} setValue={setCareer} /></td>
            <td>{t("cardEditor.investor.homeland")}</td>
            <td><CellInput value={avatar["detail.descriptor.homeland"]} setValue={setHomeland} /></td>
          </tr>
        </tbody>
      </table>
    </>
  )
}