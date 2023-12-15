import { IAvatar } from "../../bindings/IAvatar";

export default () : IAvatar => {
  return {
    id : 0,
    stage_uuid : "",
    owner : 0,
    name : "new avatar",
    detail : {
      status : {
        hp : 0,
        mp : 0,
        san : 0,
        mental_status : "Lucid",
        health_status : "Healthy"
      },
      attrs : {
        str : 0,
        dex : 0,
        pow : 0,
        con : 0,
        app : 0,
        edu : 0,
        siz : 0,
        int : 0,
        mov : 0,
        luk : 0,
      },
      descriptor : {
        age : 0,
        career : "",
        gender : "unknown",
        homeland : ""
      },
      skills : {

      }
    }
  }
}
