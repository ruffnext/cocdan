import { createSignal } from "solid-js"
import { IReqCreateStage } from "../../../bindings/api/stage/create/IReqCreateStage"
import { IStageRule } from "../../../bindings/entity/basic/IStageRule"
import toast from "solid-toast"
import { post } from "../../../api/core"
import { useNavigate } from "@solidjs/router"

export default () => {
  const navigate = useNavigate()
  const [stage, setStage] = createSignal<Partial<IReqCreateStage>>({
    rule: "CoC7th",
    description: ""
  })
  const [dangerForm, setDangerForm] = createSignal<"Title" | "None">("None")

  const setTitle = (title: string) => {
    setStage({ ...stage(), title })
    if (!title || title === "") {
      setDangerForm("Title")
    } else if (dangerForm() === "Title") {
      setDangerForm("None")
    }
  }

  const setDescription = (description: string) => {
    setStage({ ...stage(), description })
  }

  const setRule = (rule: IStageRule) => {
    setStage({ ...stage(), rule })
  }

  const getRuleSelected = (rule: IStageRule) => {
    return stage().rule === rule
  }

  async function createStage() {
    const stageContent = stage();
    const stageData: IReqCreateStage = {
      title: "",
      description: "",
      rule: "CoC7th"
    }
    setDangerForm("None")
    if (!stageContent.title || stageContent.title === "") {
      toast.error("舞台名称不能为空")
      setDangerForm("Title")
      return
    } else {
      stageData.title = stageContent.title
    }
    if (!stageContent.rule) {
      toast.error("请选择规则")
      return
    } else {
      stageData.rule = stageContent.rule
    }
    if (!stageContent.description) {
      stageContent.description = ""
    }
    stageData.description = stageContent.description
    const resp = await post('/stage/new', stageData, undefined, false);
    if ("Ok" in resp) {
      toast.success("创建成功")
      navigate(`/stage/${resp.Ok.raw_id}/perform`)
    } else {
      toast.error("创建失败")
    }
  }

  return (
    <div class="container mx-auto mt-8">
      <p class="text-2xl font-bold">创建舞台</p>
      <p>舞台是跑团中所有传奇发生的地方，创建一个舞台，并在上面布置PC和NPC。</p>
      <hr />
      <div class="grid grid-cols-3 gap-4">
        <div class="grow">
          <div class="field">
            <label class="label">舞台名称*</label>
            <div class="control">
              <input
                on:blur={(e) => setTitle(e.target.value)}
                class={`input ${dangerForm() === "Title" ? "is-danger" : ""}`}
                type="text"
                placeholder="例如：恐怖小镇"
              />
            </div>
          </div>
          <div class="field">
            <label class="label">描述（可选）</label>
            <div class="control">
              <input
                on:blur={(e) => setDescription(e.target.value)}
                class="input"
                type="text"
                placeholder="e.g Alex Smith"
              />
            </div>
          </div>
        </div>
        <div class="column is-one-third stage-image-holder">
          <figure class="image is-128x128 stage-image">
            <img src="/img/128x128.png" />
          </figure>
        </div>
      </div>

      <hr />
      <div class="ml-4">
        <div class="flex mt-2" on:click={() => setRule("CoC7th")}>
          <div class="flex mt-auto mb-auto">
            <input id="coc7th-radio" type="radio" checked={getRuleSelected("CoC7th")} />
          </div>
          <div class="ml-2">
            <p class="text-xl font-bold">CoC 7 版规则</p>
            <p class="text-sm">目前最常用的规则</p>
          </div>
        </div>
        <div class="flex mb-2" on:click={() => setRule("CoC6th")}>
          <div class="flex mt-auto mb-auto">
            <input id="coc7th-radio" type="radio" checked={getRuleSelected("CoC6th")} />
          </div>
          <div class="ml-2">
            <p class="text-xl font-bold">CoC 6 版规则</p>
            <p class="text-sm">部分早期模组所采用的规则</p>
          </div>
        </div>
      </div>
      <hr />
      <div class="w-full flex">
        <div class="flex-grow"></div>
        <button class="w-24 h-12 border-2 rounded-md border-gray-500 bg-green-300 hover:bg-green-500" on:click={createStage}>创建舞台</button>
      </div>
    </div>
  )
}
