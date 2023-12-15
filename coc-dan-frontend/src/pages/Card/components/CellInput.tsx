import { createSignal } from "solid-js"

interface Props {
  setValue : (val : string) => string,
  value : string
}

export default (props : Props) => {
  const [value, setValue] = createSignal<string>(props.value)
  const onBlur = (e : any) => {
    const ret = props.setValue(e.target.value)
    setValue(ret)
  }
  return (
    <input type="text" value={value()} onblur={onBlur} onChange={(e) => setValue(e.target.value)}/>
  )
}
