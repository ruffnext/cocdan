import "./style.css"
import { useSession } from "../Login/context"

export default () => {
  const { session } = useSession()
  return (
    <h1 class="text-3xl font-bold underline">
      Hello world!
    </h1>
  )
}
