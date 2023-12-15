import { IHealthStatus } from "../../bindings/avatar/IHealthStatus";
import { IMentalStatus } from "../../bindings/avatar/IMentalStatus";

export const HealthStatus : IHealthStatus[] = [
  "Healthy",
  "Ill",
  "Injured",
  "Critical",
  "Dead"
]

export const MentalStatus : IMentalStatus[] = [
  "Lucid",
  "Fainting",
  "TemporaryInsanity",
  "IntermittentInsanity",
  "PermanentInsanity"
]
