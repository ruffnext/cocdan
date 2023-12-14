import { globalUser } from "../../core/user"
import "./style.css"
import RecentActivity from "./RecentActivity/Component"
import LastStage from "./LastStage/Component"

globalUser().list_stages().then((res) => {
  console.log(res)
})

export default () => {
  return (
    <div id="home-main-container" class="box-shadow columns radius">


      <div class="column is-one-third">
        <div id="user-info-card" class="main-card-container radius">
          <figure id="user-header" class="image is-128x128">
            <img class="is-rounded" src={globalUser().raw.header}></img>
          </figure>
          <div id="user-name" class="title">username</div>
        </div>

      <LastStage />
      </div>


      <div class="column is-two-thirds">
        <RecentActivity />
      </div>
    </div>
  )
}
