import { useI18N } from "../../../core/i18n";
import { getCardI18n } from "../../../core/card/i18n/core";
import { useAvatar } from "../context";
import "./table.css";
import { IAvatar } from "../../../bindings/IAvatar";

export default () => {
  const { avatar, setAvatar } = useAvatar();
  const t = getCardI18n(useI18N()());

  return (
    <div class="box-shadow" style="width: 100%">
      <p class="box-edit-header">{t("Weapons.title")}</p>
      <table style="width: 100%">
        <tbody>
          <tr>
            <th>{t("Weapons.name")}</th>
            <th>{t("Weapons.type")}</th>
            <th>{t("Weapons.skill")}</th>
            <th>{t("Weapons.successRate")}</th>
            <th>{t("Weapons.damage")}</th>
            <th>{t("Weapons.range")}</th>
            <th>{t("Weapons.puncture")}</th>
            <th>{t("Weapons.frequency")}</th>
            <th>{t("Weapons.loadingCapacity")}</th>
            <th>{t("Weapons.fault")}</th>
          </tr>
          <tr>
            <td>todo</td>
            <td>todo</td>
            <td>todo</td>
            <td>todo</td>
            <td>todo</td>
            <td>todo</td>
            <td>todo</td>
            <td>todo</td>
            <td>todo</td>
            <td>todo</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
};
