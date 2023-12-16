import { useParams } from "@solidjs/router";
import { IAvatar } from "../../bindings/IAvatar";
import { createSignal } from "solid-js";
import newEmpty from "../../core/card/new-empty";
import Info from "./components/Info";
import Attrs from "./components/Attrs";
import Status from "./components/Status";
import { AvatarProvider } from "./context";
import "./style.css"
import * as flat from "flatten-type"
import OccupationalSkillEditor from "./components/Skills/OccupationalSkillEditor";

enum PageStatus {
  IsLoading,
  LoadDone,
  LoadError,
}

export default () => {
  const params = useParams()
  const initialAvatar = flat.flatten(newEmpty())
  const [avatar, setAvatar] = createSignal(initialAvatar)

  const [pageStatus, setPageStatus] = createSignal<PageStatus>(params.id == "new" ? PageStatus.LoadDone : PageStatus.IsLoading)

  async function load_avatar(avatarId: string): Promise<IAvatar> {
    return newEmpty()
  }

  if (params.id != "new") {
    load_avatar(params.id).then((val: IAvatar) => {
      setAvatar(flat.flatten(val))
    })
  } else {
  }

  return (
    <div id="container" style="margin : auto">
      <AvatarProvider avatar={avatar()}>
        <div>
          <div style="margin-top: 1em; display : flex">
            <div style="margin : 1em; display : flex; flex-direction : column; width : 75%">
              <div style="display : flex">
                <div id="avatar-info" class="box-shadow"><Info /></div>
                <div id="avatar-attrs" class="box-shadow"><Attrs /></div>
              </div>
              <div id="avatar-status" class="box-shadow"><Status /></div>
            </div>
            <div style="height : auto; width : 25%">
              <figure id="avatar-header" class="box-shadow">
                <img style="width: 100%; height: auto;" src="/img/default_avatar_header.png" />
              </figure>
            </div>
          </div>
          <div style="display : flex; margin : 1em">
            <OccupationalSkillEditor />
          </div>
        </div>
      </AvatarProvider>
    </div>
  )
}
