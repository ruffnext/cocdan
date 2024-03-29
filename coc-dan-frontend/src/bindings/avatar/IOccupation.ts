// This file was generated by [ts-rs](https://github.com/Aleph-Alpha/ts-rs). Do not edit this file manually.
import type { ICharacteristicEnum } from "./ICharacteristicEnum";
import type { IEraEnum } from "../IEraEnum";
import type { IOccupationalSkill } from "./IOccupationalSkill";

export interface IOccupation { name: string, credit_rating: [number, number], era: IEraEnum, characteristics: Array<ICharacteristicEnum>, occupational_skills: Array<IOccupationalSkill>, }