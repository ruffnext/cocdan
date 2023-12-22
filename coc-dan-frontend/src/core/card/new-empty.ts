import { IAvatar } from "../../bindings/IAvatar";
import { deepClone } from "../utils";
import { ICharacteristics } from "../../bindings/avatar/ICharacteristics";
import { WEAPONS, getOccupationOrDefault, initOccupationalSkill } from "./resource";
import { IEquipment } from "../../bindings/IEquipment";

export default () : IAvatar => {
  const characteristics : ICharacteristics = {
    str : 50,
    dex : 50,
    pow : 50,
    con : 50,
    app : 50,
    edu : 50,
    siz : 50,
    int : 50,
    mov : 50,
    luk : 50,
    mov_adj : null
  }
  const occupation = deepClone(getOccupationOrDefault("Secretary"))
  const occupationalSkills = initOccupationalSkill(characteristics, occupation)
  const weapons : Array<IEquipment> = []

  weapons.push({
    name : "Punch",
    item : {
      // @ts-ignore
      "Weapon" : deepClone(WEAPONS.get("Punch"))
    }
  })
  
  weapons.push({
    name : "Small Knife",
    item : {
      // @ts-ignore
      "Weapon" : deepClone(WEAPONS.get("Small Knife"))
    }
  })

  
  return {
    id : 0,
    stage_uuid : "",
    owner : 0,
    name : "Chris Redfield",
    detail : {
      status : {
        hp : 10,
        mp : 10,
        san : 50,
        hp_loss : 0,
        mp_loss : 0,
        san_loss : 0,
        mental_status : "Lucid",
        health_status : "Healthy"
      },
      characteristics : characteristics,
      descriptor : {
        age : 27,
        gender : "Other",
        homeland : "Homeland"
      },
      skills : occupationalSkills,
      occupation : occupation,
      equipments : weapons
    }
  }
}
