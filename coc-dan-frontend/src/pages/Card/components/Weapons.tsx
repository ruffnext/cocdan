import { useI18N } from "../../../core/i18n";
import { getCardI18n } from "../../../core/card/i18n/core";
import { useAvatar } from "../context";
import "./table.css";
import CellInput from "./CellInput";


import { IWeapon } from "../../../bindings/weapon/IWeapon";
import { For } from "solid-js";
import { IEquipment } from "../../../bindings/IEquipment";
import { IEquipmentItem } from "../../../bindings/IEquipmentItem";
import { genSkillI18n } from "../../../core/skill/i18n/core";
import { getWeaponI18n, getWeaponName } from "../../../core/weapon/i18n/core";
import { SKILLS, WEAPONS } from "../../../core/card/resource";
import { ISkill } from "../../../bindings/avatar/ISkill";
import { ISkillAssigned } from "../../../bindings/avatar/ISkillAssigned";

export default () => {
  const { avatar, setAvatar } = useAvatar();
  const i18n = useI18N()();
  const t = getCardI18n(i18n);
  const ts = genSkillI18n(i18n)
  const tw = getWeaponI18n(i18n)

  const setName = (i: number, val: string): string => {
    // @ts-ignore
    setAvatar("detail", "equipments", i, "item", "Weapon", "name", val);
    return val;
  };

  // const setSkillName = (i: number, val: ISkill)  => {
  //   // @ts-ignore
  //   setAvatar("detail", "equipments", i, "item", "Weapon", "skill_name", val.name);
  //   return getSkillI18nName(val.name);
  // }

  // const setReliability = (i:number, val: number): number => {
  //   // @ts-ignore
  //   setAvatar("detail", "equipments", i, "item", "Weapon", "reliability", val);
  //   return val;
  // }

  const insertWeapon = () => {
    const weapon : IWeapon = {
      name : "",
      skill_name: "",
      damage: {
        dice: "",
        side_effect: "Burning",
      },
      range: "Melee",
      impale: false,
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
    setAvatar("detail", "equipments", (equipments: Array<IEquipment>) => [...equipments, equipment]);
  };

  // filter out all weapons of avatar
  const getWeapons = () : Array<[IWeapon, string]> => {
    const res : Array<[IWeapon, string]> = []
    for (const key in avatar.detail.equipments) {
      const weapon = avatar.detail.equipments[key].item
      if (weapon != undefined && 'Weapon' in weapon) {
        res.push([weapon.Weapon, key])
      }
    }
    return res
  }

  const getSkillI18nName = (name : string) : string => {
    // @ts-ignore
    return ts("skill." + name + ".name") || name
  }

  const weapons :Array<{label: string, value: IWeapon}> = []
  for (const [name, weapon] of WEAPONS) {
    weapons.push({
      label: getWeaponName(name, tw),
      value: weapon
    });
  }

  const weaponSkills: Array<{label: string, value: ISkill}> = []
  for (const [name, skill] of SKILLS) {
    weaponSkills.push({
      label: getSkillI18nName(name),
      value: skill
    });
  }

  const getWeaponSkillSuccessPossibility = (skill : string) : string => {
    const avatarSkill : ISkillAssigned | undefined = avatar.detail.skills[skill]
    // if avatar has learnt this skill
    if (avatarSkill != undefined) {
      return (avatarSkill.initial + avatarSkill.interest_skill_point + avatarSkill.occupation_skill_point).toFixed(0)
    } 

    // otherwise use default skill
    else {
      const defaultSkill : ISkill | undefined = SKILLS.get(skill)
      if (defaultSkill != undefined) {
        return defaultSkill.initial.toFixed(0)
      }
    }

    // or just a zero
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
            <th>{t("weaponEditor.impale")}</th>
            <th>{t("weaponEditor.rate_of_fire")}</th>
            <th>{t("weaponEditor.ammo_capacity")}</th>
            <th>{t("weaponEditor.reliability")}</th>
          </tr>
          <For each={getWeapons()}>
            {([item, i]) => (
              <tr>
                <td>
                  <CellInput 
                    value={getWeaponName(item.name, tw)} 
                    setValue={(val) => setName(Number.parseInt(i), val)} />
                </td>
                <td>{getSkillI18nName(item.skill_name)}</td>
                <td>{getWeaponSkillSuccessPossibility(item.skill_name) + "%"}</td>
                <td>{item.damage.dice}</td>
                <td>{tw("range.name", item.range)}</td>
                <td>{tw("impale.name", item.impale)}</td>
                <td>{item.rate_of_fire.toFixed(0)}</td>
                <td>{tw("capacity.value", item.ammo_capacity)}</td>
                <td>{item.reliability}</td>
              </tr>
            )}
          </For>
          <tr>
            <td colSpan="9">
              新增武器
            </td>
          </tr>
        </tbody>
      </table>
      
    </div>
  );
};
