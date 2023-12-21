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
import { genSkillI18n } from "../../../core/skill/i18n/core";
import { getWeaponI18n, getWeaponName } from "../../../core/weapon/i18n/core";
import { SKILLS } from "../../../core/card/resource";
import { ISkill } from "../../../bindings/avatar/ISkill";
import { ISkillAssigned } from "../../../bindings/avatar/ISkillAssigned";

export default () => {
  const { avatar, setAvatar } = useAvatar();
  const i18n = useI18N()();
  const t = getCardI18n(i18n);
  const ts = genSkillI18n(i18n)
  const tw = getWeaponI18n(i18n)

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
    console.log(res)
    return res
  }

  const getSkillI18nName = (name : string) : string => {
    // @ts-ignore
    return ts("skill." + name + ".name") || name
  }

  const getWeaponSkillSuccessPossibility = (skill : string) : string => {
    const avatarSkill : ISkillAssigned | undefined = avatar.detail.skills[skill]
    if (avatarSkill != undefined) {
      return (avatarSkill.initial + avatarSkill.interest_skill_point + avatarSkill.occupation_skill_point).toFixed(0)
    } else {
      const defaultSkill : ISkill | undefined = SKILLS.get(skill)
      if (defaultSkill != undefined) {
        return defaultSkill.initial.toFixed(0)
      }
    }
    return "0"
  }

  return (
    <div class="box-shadow" style="width: 100%">
      <p class="box-edit-header">{t("weaponEditor.title")}</p>
      <table style="width: 100%">
        <tbody>
          <tr>
            <th>{t("weaponEditor.name")}</th>
            <th>{t("weaponEditor.skill")}</th>
            <th>{t("weaponEditor.successRate")}</th>
            <th>{t("weaponEditor.damage")}</th>
            <th>{t("weaponEditor.range")}</th>
            <th>{t("weaponEditor.puncture")}</th>
            <th>{t("weaponEditor.frequency")}</th>
            <th>{t("weaponEditor.loadingCapacity")}</th>
            <th>{t("weaponEditor.fault")}</th>
          </tr>
          <For each={getWeapons()}>
            {(item, _) => (
              <tr>
                <td>
                  <CellInput 
                    value={getWeaponName(item.name, tw)} 
                    setValue={setName} />
                </td>
                <td>{getSkillI18nName(item.skill_name)}</td>
                <td>{getWeaponSkillSuccessPossibility(item.skill_name) + "%"}</td>
                <td>{item.damage.dice}</td>
                <td>{tw("range.name", item.range)}</td>
                <td>{tw("penetration.name", item.penetration)}</td>
                <td>{item.rate_of_fire.toFixed(0)}</td>
                <td>{tw("capacity.value", item.ammo_capacity)}</td>
                <td>
                  <InlineInput
                    value={item.reliability}
                    upperLimit={101}
                    setValue={setReliability} />
                </td>
              </tr>
            )}
          </For>
          <tr>
            <td colSpan="9" onClick={() => insertWeapon()}>Add</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
};
