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
    const fieldStr : any = "detail.attrs." + field
    return (e : string ) : string => {
      const res = parseIntOrDefault(e, avatar[fieldStr], upperLimit)
      setAvatar(fieldStr, res)
      if (field == "con" || field == "siz") {
        const max_hp = maxHP(avatar["detail.attrs.con"], avatar["detail.attrs.siz"])
        var hp_loss = avatar["detail.status.hp_loss"] == 0 ? 0 : max_hp - avatar["detail.status.hp"]
        if (hp_loss < 0) {
          hp_loss = 0
        }
        setAvatar("detail.status.hp", max_hp - hp_loss)
        setAvatar("detail.status.hp_loss", hp_loss)
      } else if (field == "pow") {
        const max_san = maxSan(avatar["detail.attrs.pow"])
        var san_loss = avatar["detail.status.san_loss"] == 0 ? 0 : max_san - avatar["detail.status.san"]
        if (san_loss < 0) {
          san_loss = 0
        }
        setAvatar("detail.status.san", max_san - san_loss)
        setAvatar("detail.status.san_loss", san_loss)

        const max_mp = maxMP(avatar["detail.attrs.pow"])
        var mp_loss = avatar["detail.status.mp_loss"] == 0 ? 0 : max_mp - avatar["detail.status.mp"]
        if (mp_loss < 0) {
          mp_loss = 0
        }
        setAvatar("detail.status.mp", max_mp - mp_loss)
        setAvatar("detail.status.mp_loss", mp_loss)
      }
      return res.toFixed(0)  
    }
  }

  return (
    <>
      <p class="box-edit-header">{t('attribute.title')}</p>
      <table>
        <tbody>
          <tr>
            <td rowSpan="2" style="width : 13%;">{t('attribute.str')}</td>
            <td rowspan="2" style="width : 13%;"><CellInput value={avatar["detail.attrs.str"].toFixed(0)} setValue={setAttr('str')} /></td>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar["detail.attrs.str"] / 2)).toFixed()}</td>

            <td rowSpan="2" style="width : 13%;">{t('attribute.dex')}</td>
            <td rowspan="2" style="width : 13%;"><CellInput value={avatar["detail.attrs.dex"].toFixed(0)} setValue={setAttr('dex')} /></td>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar["detail.attrs.dex"] / 2)).toFixed()}</td>

            <td rowSpan="2" style="width : 13%;">{t('attribute.pow')}</td>
            <td rowspan="2" style="width : 13%;"><CellInput value={avatar["detail.attrs.pow"].toFixed(0)} setValue={setAttr('pow', 999)} /></td>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar["detail.attrs.pow"] / 2)).toFixed()}</td>
          </tr>
          <tr>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar["detail.attrs.str"] / 5)).toFixed()}</td>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar["detail.attrs.dex"] / 5)).toFixed()}</td>
            <td class="is-small" style="width : 7.33%;">{Math.floor((avatar["detail.attrs.pow"] / 5)).toFixed()}</td>
          </tr>
          <tr>
            <td rowSpan="2">{t('attribute.con')}</td>
            <td rowspan="2"><CellInput value={avatar["detail.attrs.con"].toFixed(0)} setValue={setAttr('con')} /></td>
            <td class="is-small">{Math.floor((avatar["detail.attrs.con"] / 2)).toFixed()}</td>

            <td rowSpan="2">{t('attribute.app')}</td>
            <td rowspan="2"><CellInput value={avatar["detail.attrs.app"].toFixed(0)} setValue={setAttr('app')} /></td>
            <td class="is-small">{Math.floor((avatar["detail.attrs.app"] / 2)).toFixed()}</td>

            <td rowSpan="2">{t('attribute.edu')}</td>
            <td rowspan="2"><CellInput value={avatar["detail.attrs.edu"].toFixed(0)} setValue={setAttr('edu')} /></td>
            <td class="is-small">{Math.floor((avatar["detail.attrs.edu"] / 2)).toFixed()}</td>
          </tr>
          <tr>
            <td class="is-small">{Math.floor((avatar["detail.attrs.con"] / 5)).toFixed()}</td>
            <td class="is-small">{Math.floor((avatar["detail.attrs.app"] / 5)).toFixed()}</td>
            <td class="is-small">{Math.floor((avatar["detail.attrs.edu"] / 5)).toFixed()}</td>
          </tr>

          <tr>
            <td rowSpan="2">{t('attribute.siz')}</td>
            <td rowspan="2"><CellInput value={avatar["detail.attrs.siz"].toFixed(0)} setValue={setAttr('siz')} /></td>
            <td class="is-small">{Math.floor((avatar["detail.attrs.siz"] / 2)).toFixed()}</td>

            <td rowSpan="2">{t('attribute.int')}</td>
            <td rowspan="2"><CellInput value={avatar["detail.attrs.int"].toFixed(0)} setValue={setAttr('int')} /></td>
            <td class="is-small">{Math.floor((avatar["detail.attrs.int"] / 2)).toFixed()}</td>

            <td rowSpan="2">{t('attribute.mov')}</td>
            <td rowSpan="2">{ avatar["detail.attrs.int"].toFixed(0) }</td>
            <td class="is-small">Adj</td>
          </tr>
          <tr>
            <td class="is-small">{Math.floor((avatar["detail.attrs.siz"] / 5)).toFixed()}</td>
            <td class="is-small">{Math.floor((avatar["detail.attrs.app"] / 5)).toFixed()}</td>
            <td class="is-small">{avatar["detail.attrs.mov_adj"] == null ? "?" : adjMovStr(avatar["detail.attrs.mov_adj"])}</td>
          </tr>
        </tbody>
      </table>
    </>
  )
}