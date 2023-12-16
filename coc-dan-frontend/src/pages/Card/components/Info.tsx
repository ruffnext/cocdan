import { useI18N } from "../../../core/i18n"
import { useAvatar } from "../context"
import { getCardI18n } from "../../../core/card/i18n/core"
import CellInput from "./CellInput"
import Dropdown from "../../../components/Dropdown/Component"
import "./table.css"
import { OCCUPATIONS, getOccupationOrDefault } from "../../../core/card/resource"
import { Genders } from "../../../core/card/enums"

export default () => {
  const { avatar, setAvatar } = useAvatar()  
  const t = getCardI18n(useI18N()())

  const setName = (val: string): string => {
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

  const getOccupationName = (name : string) : string => {
    // @ts-ignore
    const res = t("occupation." + name + ".name")
    if (res == undefined) {
      return name
    } else {
      // @ts-ignore
      return res
    }
  }

  const setOccupation = (val : string) : string => {
    const occupation = getOccupationOrDefault(val)
    setAvatar("detail.occupation.name", occupation.name)
    setAvatar("detail.occupation.additional_skill_num", occupation.additional_skill_num)
    setAvatar("detail.occupation.attribute", occupation.attribute)
    setAvatar("detail.occupation.credit_rating", occupation.credit_rating)
    setAvatar("detail.occupation.era", occupation.era)
    setAvatar("detail.occupation.occupational_skills", occupation.occupational_skills)
    return getOccupationName(val)
  }

  const occupations : Array<{label : string, value : string}> = []
  for (const [name, _] of OCCUPATIONS) {
    occupations.push({label : getOccupationName(name), value : name})
  }

  const genders : Array<{label : string, value : string}> = []
  for (const val of Genders) {
    // @ts-ignore
    genders.push({label : t("investor.genderEnum." + val), value : val})
  }

  const getGenderName = (val : string) : string => {
    // @ts-ignore
    return t("investor.genderEnum." + val)
  }

  const setGender = (val : string) : string => {
    // @ts-ignore
    setAvatar("detail.descriptor.gender", val)
    return getGenderName(val)
  }
  
  return (
    <>
      <p class="box-edit-header">{t("investor.title")}</p>
      <table>
        <tbody>
          <tr>
            <td>{t("investor.name")}</td>
            <td colSpan="3"><CellInput value={avatar.name} setValue={setName} /></td>
          </tr>
          <tr>
            <td style="width : 17%">{t("investor.gender")}</td>
            <td style="width : 33%">
              <Dropdown items={genders} initialLabel={getGenderName(avatar["detail.descriptor.gender"])} setValue={setGender}/>
            </td>
            <td style="width : 17%">{t("investor.age")}</td>
            <td style="width : 33%"><CellInput value={avatar["detail.descriptor.age"].toFixed(0)} setValue={setAge} /></td>
          </tr>
          <tr>
            <td>{t("investor.career")}</td>
            <td>
              <Dropdown items={occupations} initialLabel={getOccupationName(avatar["detail.occupation.name"])} setValue={setOccupation} />
            </td>
            <td>{t("investor.homeland")}</td>
            <td><CellInput value={avatar["detail.descriptor.homeland"]} setValue={setHomeland} /></td>
          </tr>
        </tbody>
      </table>
    </>
  )
}