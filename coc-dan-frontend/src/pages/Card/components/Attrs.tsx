import { maxHP, maxMP, maxSan } from "../../../core/card/calc"
import { useI18N } from "../../../core/i18n"
import { useAvatar } from "../context"
import { getCardI18n } from "../../../core/card/i18n/core"
import CellInput from "./CellInput"

function parseIntOrDefault(val: string, defaultVal: number, upperLimit = 100) {
  const res = parseInt(val)
  if (isNaN(res) || res < 0 || res > upperLimit) {
    return defaultVal
  } else {
    return res
  }
}

function adjMovStr(val : number) : string {
  if (val > 0) {
    return "+" + val.toFixed(0)
  } else if (val == 0) {
    return "±" + val.toFixed(0)
  } else {
    return val.toFixed(0)
  }
}

export default () => {
  const { avatar, setAvatar } = useAvatar()
  const t = getCardI18n(useI18N()())
  const setAttr = (field : any, upperLimit = 100) => {
    return (e : string ) : string => {
      // @ts-ignore
      const res = parseIntOrDefault(e, avatar.detail.characteristics[field], upperLimit)
      setAvatar("detail", "characteristics", field, res)
      if (field == "con" || field == "siz") {
        const max_hp = maxHP(avatar.detail.characteristics.con, avatar.detail.characteristics.siz)
        var hp_loss = avatar.detail.status.hp_loss == 0 ? 0 : max_hp - avatar.detail.status.hp
        if (hp_loss < 0) {
          hp_loss = 0
        }
        setAvatar("detail", "status", "hp", max_hp - hp_loss)
        setAvatar("detail", "status", "hp_loss", hp_loss)
      } else if (field == "pow") {
        const max_san = maxSan(avatar.detail.characteristics.pow)
        var san_loss = avatar.detail.status.san_loss == 0 ? 0 : max_san - avatar.detail.status.san
        if (san_loss < 0) {
          san_loss = 0
        }
        setAvatar("detail", "status", "san", max_san - san_loss)
        setAvatar("detail", "status", "san_loss", san_loss)

        const max_mp = maxMP(avatar.detail.characteristics.pow)
        var mp_loss = avatar.detail.status.mp_loss == 0 ? 0 : max_mp - avatar.detail.status.mp
        if (mp_loss < 0) {
          mp_loss = 0
        }
        setAvatar("detail", "status", "mp", max_mp - mp_loss)
        setAvatar("detail", "status", "mp_loss", mp_loss)
      }
      return res.toFixed(0)  
    }
  }

  return (
    <>
      <p class="box-edit-header">{t('characteristic.title')}</p>
      <table>
        <tbody>
          <tr>
            <td rowSpan="2" style="width : 13%;">{t('characteristic.str')}</td>
            <td rowspan="2" style="width : 13%;"><CellInput value={avatar.detail.characteristics.str.toFixed(0)} setValue={setAttr('str')} /></td>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar.detail.characteristics.str / 2)).toFixed()}</td>

            <td rowSpan="2" style="width : 13%;">{t('characteristic.dex')}</td>
            <td rowspan="2" style="width : 13%;"><CellInput value={avatar.detail.characteristics.dex.toFixed(0)} setValue={setAttr('dex')} /></td>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar.detail.characteristics.dex / 2)).toFixed()}</td>

            <td rowSpan="2" style="width : 13%;">{t('characteristic.pow')}</td>
            <td rowspan="2" style="width : 13%;"><CellInput value={avatar.detail.characteristics.pow.toFixed(0)} setValue={setAttr('pow', 999)} /></td>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar.detail.characteristics.pow / 2)).toFixed()}</td>
          </tr>
          <tr>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar.detail.characteristics.str / 5)).toFixed()}</td>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar.detail.characteristics.dex / 5)).toFixed()}</td>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar.detail.characteristics.pow / 5)).toFixed()}</td>
          </tr>
          <tr>
            <td rowSpan="2">{t('characteristic.con')}</td>
            <td rowspan="2"><CellInput value={avatar.detail.characteristics.con.toFixed(0)} setValue={setAttr('con')} /></td>
            <td class="is-small">{Math.floor((avatar.detail.characteristics.con / 2)).toFixed()}</td>

            <td rowSpan="2">{t('characteristic.app')}</td>
            <td rowspan="2"><CellInput value={avatar.detail.characteristics.app.toFixed(0)} setValue={setAttr('app')} /></td>
            <td class="is-small">{Math.floor((avatar.detail.characteristics.app / 2)).toFixed()}</td>

            <td rowSpan="2">{t('characteristic.edu')}</td>
            <td rowspan="2"><CellInput value={avatar.detail.characteristics.edu.toFixed(0)} setValue={setAttr('edu')} /></td>
            <td class="is-small">{Math.floor((avatar.detail.characteristics.edu / 2)).toFixed()}</td>
          </tr>
          <tr>
            <td class="is-small">{Math.floor((avatar.detail.characteristics.con / 5)).toFixed()}</td>
            <td class="is-small">{Math.floor((avatar.detail.characteristics.app / 5)).toFixed()}</td>
            <td class="is-small">{Math.floor((avatar.detail.characteristics.edu / 5)).toFixed()}</td>
          </tr>

          <tr>
            <td rowSpan="2">{t('characteristic.siz')}</td>
            <td rowspan="2"><CellInput value={avatar.detail.characteristics.siz.toFixed(0)} setValue={setAttr('siz')} /></td>
            <td class="is-small">{Math.floor((avatar.detail.characteristics.siz / 2)).toFixed()}</td>

            <td rowSpan="2">{t('characteristic.int')}</td>
            <td rowspan="2"><CellInput value={avatar.detail.characteristics.int.toFixed(0)} setValue={setAttr('int')} /></td>
            <td class="is-small">{Math.floor((avatar.detail.characteristics.int / 2)).toFixed()}</td>

            <td rowSpan="2">{t('characteristic.mov')}</td>
            <td rowSpan="2">{ avatar.detail.characteristics.int.toFixed(0) }</td>
            <td class="is-small">Adj</td>
          </tr>
          <tr>
            <td class="is-small">{Math.floor((avatar.detail.characteristics.siz / 5)).toFixed()}</td>
            <td class="is-small">{Math.floor((avatar.detail.characteristics.app / 5)).toFixed()}</td>
            <td class="is-small">{avatar.detail.characteristics.mov_adj == null ? "?" : adjMovStr(avatar.detail.characteristics.mov_adj)}</td>
          </tr>
        </tbody>
      </table>
    </>
  )
}