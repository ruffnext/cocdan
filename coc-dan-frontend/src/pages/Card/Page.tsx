import { useParams } from "@solidjs/router";
import { IAvatar } from "../../bindings/IAvatar";
import { createSignal } from "solid-js";
import newEmpty from "../../core/card/new-empty";
import Info from "./components/Info";
import Attrs from "./components/Attrs";
import Status from "./components/Status";
import { AvatarProvider } from "./context";
import "./style.css"
import * as i18n from "@solid-primitives/i18n";

enum PageStatus {
  IsLoading,
  LoadDone,
  LoadError,
}

// @ts-ignore
type FlattenAvatar = Flatten<IAvatar>


export default () => {
  const params = useParams()
  const initialAvatar = i18n.flatten(newEmpty() as any)
  const [avatar, setAvatar] = createSignal<FlattenAvatar>(initialAvatar)

  const [pageStatus, setPageStatus] = createSignal<PageStatus>(params.id == "new" ? PageStatus.LoadDone : PageStatus.IsLoading)

  async function load_avatar(avatarId: string): Promise<IAvatar> {
    return newEmpty()
  }

  if (params.id != "new") {
    load_avatar(params.id).then((val: IAvatar) => {
      setAvatar(i18n.flatten(val as any))
    })
  } else {
    console.log("new avatar")
  }

  return (
    <div id="container" class="container is-max-widescreen">
      <AvatarProvider avatar={avatar()}>
        <div class="tile is-ancestor" style="margin-top: 1em;">
          <div style="margin : 1em;" class="tile is-vertical is-9">
            <div class="tile">
              <div id="avatar-info" class="box-shadow"><Info /></div>
              <div id="avatar-attrs" class="box-shadow"><Attrs /></div>
            </div>
            <div id="avatar-status" class="tile box-shadow"><Status /></div>
          </div>
          <div style="height : 10em" class="tile">
            <figure id="avatar-header" class="box-shadow">
              <img style="width: 100%; height: auto;" src="/img/default_avatar_header.png" />
            </figure>
          </div>
        </div>

      </AvatarProvider>
    </div>
  )
}

// <div class="tile is-ancestor">

// </div>
// <div class="status-box"></div>
// <div class="skill-box"></div>
