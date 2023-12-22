import { IAmmoCapacity } from "../../../bindings/weapon/IAmmoCapacity"
import { IWeaponRange } from "../../../bindings/weapon/IWeaponRange"
import { IWeaponI18NRaw } from "./def"

const dict : IWeaponI18NRaw = {
  "weapon" : {
    "Punch" : {
      "name" : "拳头"
    },
    "Small Knife" : {
      "name" : "小刀"
    }
  },
  "range" : {
    "name" : (range : IWeaponRange) : string => {
      if (range == "Melee") {
        return "接触"
      } else if ("Meter" in range) {
        return range.Meter.toFixed(0) + " m"
      } else {
        return range.Formula + " m"
      }
    }
  },
  "capacity" : {
    "value" : (val : IAmmoCapacity) : string => {
      if (val == "None") {
        return "--"
      } else if (val == "SingleUse") {
        return "单次使用"
      } else {
        return val.Identity.toFixed(0)
      }
    }
  },
  "impale" : {
    "name" : (val : boolean) : string => {
      if (val == true) {
        return "是"
      } else {
        return "否"
      }
    }
  }
}


export default dict