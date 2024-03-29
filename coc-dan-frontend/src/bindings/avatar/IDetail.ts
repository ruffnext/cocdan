// This file was generated by [ts-rs](https://github.com/Aleph-Alpha/ts-rs). Do not edit this file manually.
import type { ICharacteristics } from "./ICharacteristics";
import type { IDescriptor } from "./IDescriptor";
import type { IEquipment } from "../IEquipment";
import type { IOccupation } from "./IOccupation";
import type { ISkillAssigned } from "./ISkillAssigned";
import type { IStatus } from "./IStatus";

export interface IDetail { status: IStatus, characteristics: ICharacteristics, descriptor: IDescriptor, skills: Record<string, ISkillAssigned>, occupation: IOccupation, equipments: Array<IEquipment>, }