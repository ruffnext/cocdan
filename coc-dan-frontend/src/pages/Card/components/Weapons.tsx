import { useI18N } from "../../../core/i18n";
import { getCardI18n } from "../../../core/card/i18n/core";
import { useAvatar } from "../context";
import "./table.css";
import CellInput from "./CellInput";

import InlineInput from "../../../components/InlineInput";
import { IWeapon } from "../../../bindings/weapon/IWeapon";
import { For } from "solid-js";
import { IEquipment } from "../../../bindings/IEquipment";
import { IEquipmentItem } from "../../../bindings/IEquipmentItem";

export default () => {
  const { avatar, setAvatar } = useAvatar();
  const t = getCardI18n(useI18N()());
  const setName = (val: string): string => {
    return val;
  };
  
  const setCategory = (val: string): string => {
    return val;
  };

  const setRateOfFire = (val: number): number => {
    return val;
  }

  const setReliability = (val: number): number => {
    return val;
  }

  const insertWeapon = () => {
    const weapon : IWeapon = {
      name : "",
      skill_name: "",
      damage: {
        dice: "",
        side_effect: "Burning",
      },
      range: "Melee",
      penetration: false,
      rate_of_fire: 0,
      ammo_capacity: "None",
      reliability: 0,
      era: "None",
      price: 0,
      category: []
    };
    const weaponEquipment : IEquipmentItem = {
      "Weapon" : weapon
    }
    const equipment : IEquipment = {
      name : "weapon name",
      item : weaponEquipment
    }
    setAvatar("detail", "equipments", [equipment]);
  };

  const getWeapons = () : Array<IWeapon> => {
    const res : Array<IWeapon> = []
    for (const key in avatar.detail.equipments) {
      const weapon = avatar.detail.equipments[key].item
      if (weapon != undefined && 'Weapon' in weapon) {
        res.push(weapon.Weapon)
      }
    }
    return res
  }

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
          <For each={getWeapons()}>
            {(item, _) => (
              <tr>
                <td>
                  <CellInput 
                    value={item.name} 
                    setValue={setName} />
                </td>
                <td>
                  <CellInput
                    value={item.category.toString()}
                    setValue={setCategory} />
                </td>
                <td>todo</td>
                <td>
                  <InlineInput
                    value={item.reliability}
                    upperLimit={100}
                    setValue={setReliability} />
                </td>
                <td>todo</td>
                <td>todo</td>
                <td>todo</td>
                <td>todo</td>
                <td>todo</td>
                <td>
                  <InlineInput 
                    value={item.rate_of_fire} 
                    upperLimit={100}
                    setValue={setRateOfFire} />
                </td>
              </tr>
            )}
          </For>
          <tr>
            <td colSpan="10" onClick={() => insertWeapon()}>Add</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
};
