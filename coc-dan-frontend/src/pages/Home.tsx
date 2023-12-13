import { globalUser } from "../core/user"

globalUser().list_stages().then((res) => {
  console.log(res)
})

export default () => {
  return <p>hello {globalUser().raw.name}</p>
}
