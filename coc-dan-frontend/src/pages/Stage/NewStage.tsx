import { createSignal } from "solid-js"

export default () => {
  const [stageName, setStageName] = createSignal<string>("")
  const [stageDescription, setStageDescription] = createSignal<string>("")
  const createStage = () => {
    console.log("create stage ", stageName(), stageDescription())
  }
  return (
    <div>
      <div>New Stage</div>
      <input type="text" value={stageName()} onChange={(e) => {setStageName(e.target.value)}} />
      <input type="text" value={stageDescription()} onChange={(e) => {setStageDescription(e.target.value)}} />
      <button onclick={createStage}>Create Stage</button>
    </div>
  )
}
