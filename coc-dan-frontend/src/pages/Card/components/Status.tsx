import { useI18N } from "../../../core/i18n"
import { useAvatar } from "../context"
import Dropdown from "../../../components/Dropdown/Component"
import { IHealthStatus } from "../../../bindings/avatar/IHealthStatus"
import { HealthStatus, MentalStatus } from "../../../core/card/enums"
import { IMentalStatus } from "../../../bindings/avatar/IMentalStatus"
import { maxHP, maxMP, maxSan } from "../../../core/card/calc"
import InlineInput from "../../../components/InlineInput"
import { getCardI18n } from "../../../core/card/i18n/core"

export default () => {
  const { avatar, setAvatar } = useAvatar()
  const t = getCardI18n(useI18N()())

  const setHP = (val: number): number => {
    const max_hp = maxHP(avatar.detail.attrs.con, avatar.detail.attrs.siz)
    if (val > max_hp) {
      val = max_hp
    }
    setAvatar("detail", "status", "hp", val)
    setAvatar("detail", "status", "hp_loss", max_hp - val)
    return val
  }

  const setSAN = (val: number): number => {
    const max_san = maxSan(avatar.detail.attrs.pow)
    if (val > max_san) {
      val = max_san
    }
    setAvatar("detail", "status", "san", val)
    setAvatar("detail", "status", "san_loss", max_san - val)
    return val
  }

  const setLuck = (val: number): number => {
    setAvatar("detail", "attrs", "luk", val)
    return val
  }

  const setMP = (val: number): number => {
    const max_mp = maxMP(avatar.detail.attrs.pow)
    if (val > max_mp) {
      val = max_mp
    }
    setAvatar("detail", "status", "mp", val)
    setAvatar("detail", "status", "mp_loss", max_mp - val)
    return val
  }

  const getHealthName = (e: IHealthStatus): string => {
    // @ts-ignore
    return t("healthStatus." + e)
  }

  const getMentalName = (e: IMentalStatus): string => {
    // @ts-ignore
    return t("mentalStatus." + e)
  }

  const hpStatus: Array<{ label: string, value: string }> = []
  for (const status of HealthStatus) {
    hpStatus.push({
      label: getHealthName(status),
      value: status
    })
  }
  const setHpStatus = (e: IHealthStatus): string => {
    console.log(avatar)
    setAvatar("detail", "status", "health_status", e)
    return getHealthName(e)
  }

  const mentalStatus: Array<{ label: string, value: string }> = []
  for (const status of MentalStatus) {
    mentalStatus.push({
      label: getMentalName(status),
      value: status
    })
  }
  const setMentalStatus = (e: IMentalStatus): string => {
    setAvatar("detail", "status", "mental_status", e)
    return getMentalName(e)
  }
  return (
    <table style="width : 100%;">
      <tbody>
        <tr>
          <td rowSpan="2" class="is-big" style="width : 10%;">
            {t("status.hp")}<br />
            <InlineInput 
              value={avatar.detail.status.hp} 
              upperLimit={maxHP(avatar.detail.attrs.con, avatar.detail.attrs.siz)} setValue={setHP} />
            / {maxHP(avatar.detail.attrs.con, avatar.detail.attrs.siz).toFixed(0)}
          </td>
          <td class="is-middle">{t("status.statusHp")}</td>

          <td rowSpan="2" class="is-big" style="width : 12%">
            {t("status.san")}<br />
            <InlineInput 
              value={avatar.detail.status.san} 
              upperLimit={maxSan(avatar.detail.attrs.pow)} 
              setValue={setSAN} /> 
            / {maxSan(avatar.detail.attrs.pow).toFixed(0)}
          </td>
          <td class="is-middle">{t("status.statusSan")}</td>

          <td rowSpan="2" class="is-big" style="width : 12%">
            {t("status.luk")}<br />
            <InlineInput 
              value={avatar.detail.attrs.luk} 
              upperLimit={99} 
              setValue={setLuck} /> 
            / 99</td>
          <td class="is-middle">{t("status.luckUsed")}</td>
          
          <td rowSpan="2" class="is-big" style="width : 12%">
            {t("status.mp")}<br />
            <InlineInput 
              value={avatar.detail.status.mp} 
              upperLimit={maxMP(avatar.detail.attrs.pow)} 
              setValue={setMP} />
            / {maxMP(avatar.detail.attrs.pow)}</td>
          <td class="is-middle">{t("status.mpRecovery")}</td>
          <td rowSpan="2" class="is-big" style="width : 12%">{t("status.arm")}<br /> (Place)</td>
        </tr>
        <tr>
          <td class="is-middle">
            <Dropdown items={hpStatus} initialLabel={getHealthName(avatar.detail.status.health_status)} setValue={setHpStatus}></Dropdown>
          </td>
          <td class="is-middle">
            <Dropdown items={mentalStatus} initialLabel={getMentalName(avatar.detail.status.mental_status)} setValue={setMentalStatus}></Dropdown>
          </td>
          <td class="is-middle">0</td>
          <td class="is-middle">1</td>
        </tr>
      </tbody>
    </table>
  )
}