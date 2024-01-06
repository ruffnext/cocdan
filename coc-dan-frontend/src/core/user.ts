import { IStage } from "../bindings/IStage";

export async function listMyStages () : Promise<Array<IStage>> {
  try {
    const response = await fetch("/api/stage/my_stages")
    const val = await response.json()
    return val
  } catch (error) {
    return []
  }
}
