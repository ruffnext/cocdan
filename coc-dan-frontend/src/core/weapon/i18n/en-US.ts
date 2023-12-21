import { IAmmoCapacity } from "../../../bindings/weapon/IAmmoCapacity"
import { IWeaponRange } from "../../../bindings/weapon/IWeaponRange"

const dict = {
  "weapon" : {
    "small knife" : {
      "name" : "small knife"
    }
  },
  "range" : {
    "name" : (range : IWeaponRange) : string => {
      if (range == "Melee") {
        return "Melee"
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
        return "Single Use"
      } else {
        return val.Identity.toFixed(0)
      }
    }
  },
  "penetration" : {
    "name" : (val : boolean) : string => {
      if (val == true) {
        return "Yes"
      } else {
        return "No"
      }
    }
  }
}

export default dict