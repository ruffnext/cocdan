import { IAvatar } from "../../bindings/IAvatar";
import { getOccupationOrDefault, initOccupationalSkill } from "./resource";

export default () : IAvatar => {
  const occupation = getOccupationOrDefault("Accountant")
  const occupationalSkills = initOccupationalSkill(occupation)
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
      attrs : {
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
      },
      descriptor : {
        age : 27,
        gender : "Other",
        homeland : "Homeland"
      },
      skills : occupationalSkills,
      occupation : occupation
    }
  }
}
