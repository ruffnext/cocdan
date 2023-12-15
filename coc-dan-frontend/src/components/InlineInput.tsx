import { createEffect, createSignal } from "solid-js"

interface InlineInputProps {
  value: number,
  setValue: (e: number) => number,
  upperLimit: number
}

export default (props: InlineInputProps) => {
  console.log("init")
  const [val, setVal] = createSignal(props.value.toFixed(0))
  createEffect(() => {
    setVal(props.value.toFixed(0))
  })
  const updateVal = (e: any) => {
    const fp = parseInt(e.target.value)
    if (isNaN(fp) || fp < 0 || fp > props.upperLimit) {
      console.log(props.value.toFixed(0))
      setVal(props.value.toFixed(0))
    } else {
      setVal(props.setValue(fp).toFixed(0))
    }
  }
  return (
    <input style="width : 1.5em; padding-bottom : 5px; padding-right : 6px; text-align : right;" class="is-big"
      type="text" value={val()} onBlur={updateVal} onChange={(e) => setVal(e.target.value)} />
  )
}