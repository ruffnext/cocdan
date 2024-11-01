import { useParams } from "@solidjs/router"
import { get } from "../../../api/core"
import { createResource } from "solid-js"
import { IStage } from "../../../bindings/entity/basic/IStage"
import { Switch, Match } from "solid-js"
import "./style.scss"

const StageDetail = (props: {
  stage: IStage
}) => {
  return (
    <div>
      <div>{props.stage.title}</div>
      <div>{props.stage.description}</div>
      <div id="action-container">
        
      </div>
    </div>
  )
}

const PlaceHolder = () => {
  return (
    <div>loading...</div>
  )
}

export default () => {
  const param = useParams()
  const [stage] = createResource(async (): Promise<IStage | undefined> => {
    const resp = await get('/stage/:id', { id: param.id }, true)
    if ("Ok" in resp) {
      return resp.Ok
    } else {
      return undefined
    }
  })
  return (
    <Switch fallback={<PlaceHolder />}>
      <Match when={stage.loading}><PlaceHolder /></Match>
      <Match when={stage.error}>error...</Match>
      <Match when={stage()}><StageDetail stage={stage() as IStage} /></Match>
    </Switch>
  )
}