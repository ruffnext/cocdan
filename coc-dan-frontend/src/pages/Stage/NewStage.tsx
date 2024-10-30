import { createSignal } from "solid-js"

export default () => {
  const [stageName, setStageName] = createSignal<string>("")
  const [stageDescription, setStageDescription] = createSignal<string>("")
  const createStage = () => {
    console.log("create stage ", stageName(), stageDescription())
  }
  return (
    // <div>
    //   <div class="container">New Stage</div>
    //   <input type="text" value={stageName()} onChange={(e) => {setStageName(e.target.value)}} />
    //   <input type="text" value={stageDescription()} onChange={(e) => {setStageDescription(e.target.value)}} />
    //   <button onclick={createStage}>Create Stage</button>
    // </div>
    <div>
      <div class="container is-max-tablet">
        <div class="h1">
          <p class="title">创建舞台</p>
        </div>
        <p style="margin-top:1em">舞台是跑团中所有传奇发生的地方，创建一个空白的舞台，并在上面布置PC和NPC。</p>
      </div>
    </div>
  )
}
