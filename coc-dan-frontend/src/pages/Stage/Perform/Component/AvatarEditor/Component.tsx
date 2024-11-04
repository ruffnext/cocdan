import { post } from "../../../../../api/core"
import { IAvatar } from "../../../../../bindings/entity/avatar/IAvatar"
import { IStage } from "../../../../../bindings/entity/basic/IStage"
import { useSession } from "../../../../Login/context"
import { createComputed, createSignal, Match, Show, Switch } from "solid-js"

type Props = {
  avatar: [string, IAvatar] | ["Add", undefined] | [undefined, undefined],
  stage: IStage,
  onChanged: (avatar: IAvatar) => void,
  onDelete: (avatar: IAvatar) => void,
  onAdd: (avatar: IAvatar) => void
}

export default (props: Props) => {
  const { session } = useSession()
  const [avatar, setAvatar] = createSignal<IAvatar | undefined>(undefined)
  var editType = "Edit"
  createComputed(() => {
    if (props.avatar[0] === "Add" || props.avatar[0] === undefined) {
      editType = "Add"
      const sessionContent = session()
      if (sessionContent == "IsLoading" || sessionContent == "NotLoggedIn") {
        setAvatar(undefined)
        return
      }
      if ("User" in sessionContent.session_type) {
        setAvatar({
          raw_id: '',
          stage: props.stage,
          owner: sessionContent.session_type.User,
          name: "new avatar",
          header: "",
          detail: {
            status: {
              hp: 0,
              mp: 0,
              san: 0,
              hp_loss: 0,
              mp_loss: 0,
              san_loss: 0,
              mental_status: "Lucid",
              health_status: "Healthy",
            },
            characteristics: {
              str: 0,
              dex: 0,
              con: 0,
              int: 0,
              app: 0,
              pow: 0,
              mov: 0,
              edu: 0,
              siz: 0,
              luk: 0,
              mov_adj: 0
            },
            descriptor: {
              age: 0,
              gender: "Other",
              homeland: "",
            },
            skills: {
            },
            occupation: {
              name: "Account",
              credit_rating: [0, 100],
              era: "Modern",
              characteristics: [],
              occupational_skills: []
            },
            equipments: []
          },
          creation_time: null,
          last_update_time: null
        })
      }
      return
    } else {
      editType = "Edit"
      setAvatar(props.avatar[1])
    }
  })
  const editorInner = (initialAvatar: IAvatar) => {
    const [avatar, setAvatar] = createSignal<IAvatar>(initialAvatar)
    const [selectedPage, setSelectedPage] = createSignal<"basic" | "status" | "characteristics" | "descriptor" | "skills" | "occupation" | "equipments">("basic")
    createComputed(() => {
      setAvatar(avatar())
    })
    async function onSave() {
      if (editType === "Add") {
        props.onAdd(avatar())
      } else {
        props.onChanged(avatar())
      }
    }

    async function onDelete() {
      if (editType === "Add") {
        return
      } else {
        const resp = await post('/avatar/delete', {
          raw_id: avatar().raw_id
        }, undefined, true)
        if ("Ok" in resp) {
          props.onDelete(avatar())
        }
      }
    }

    const basicInfoEditor = () => {
      return (
        <div>
          <label class="block text-gray-700 text-sm font-bold mb-2">
            Name
          </label>
          <input type="text" class="shadow mb-2 appearance-none border rounded-md w-full py-2 px-3" placeholder="Name"
            on:change={(e) => setAvatar({
              ...avatar(),
              name: e.target.value
            })}
            value={avatar().name} />

          <label class="block text-gray-700 text-sm font-bold mb-2">
            Age
          </label>
          <input type="text" class="shadow mb-2 appearance-none border rounded-md w-full py-2 px-3" placeholder="Age"
            on:change={(e) => setAvatar({
              ...avatar(),
              detail: {
                ...avatar().detail,
                descriptor: {
                  ...avatar().detail.descriptor,
                  age: parseInt(e.target.value)
                }
              }
            })}
            value={avatar().detail.descriptor.age} />

        </div>
      )
    }
    return (
      <div class="h-full w-full p-4 flex flex-col">
        <div class="flex w-full h-8 mb-4">
          <button title="basic info" class={`m-1 ${selectedPage() === "basic" ? "text-gray-800" : "text-gray-500"}`} on:click={() => setSelectedPage("basic")}>
            <svg class="h-8 w-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />  <circle cx="12" cy="7" r="4" /></svg>
          </button>
          <button title="occupation" class={`m-1 ${selectedPage() === "occupation" ? "text-gray-800" : "text-gray-500"}`} on:click={() => setSelectedPage("occupation")}>
            <svg class="h-8 w-8" width="24" height="24" viewBox="0 0 24 24"
              stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round"
              stroke-linejoin="round">
              <path stroke="none" d="M0 0h24v24H0z" />
              <rect x="3" y="7" width="18" height="13" rx="2" />
              <path d="M8 7v-2a2 2 0 0 1 2 -2h4a2 2 0 0 1 2 2v2" />
              <line x1="12" y1="12" x2="12" y2="12.01" />
              <path d="M3 13a20 20 0 0 0 18 0" />
            </svg>
          </button>
        </div>
        <div class="flex-grow overflow-y-auto">
          <Switch>
            <Match when={selectedPage() === "basic"}>
              {basicInfoEditor()}
            </Match>
            <Match when={selectedPage() === "occupation"}>
              <div>occupation</div>
            </Match>
          </Switch>
        </div>
        <div class="w-full h-10 pl-4 pr-4 mb-4">
          <button class="w-full h-full rounded-md bg-green-300 hover:bg-green-500"
            on:click={onSave}>Save</button>
        </div>
        <Show when={editType === "Edit"}>
          <div class="w-full h-10 pl-4 pr-4 mb-4">
            <button class="w-full h-full rounded-md bg-red-300 hover:bg-red-500"
              on:click={onDelete}>
              Delete
            </button>
          </div>
        </Show>
      </div>
    )
  }
  return (
    <div class="h-full w-80 bg-gray-300">
      <Switch>
        <Match when={avatar() != undefined}>
          {editorInner(avatar()!)}
        </Match>
        <Match when={avatar() == undefined}>
          <div>loading...</div>
        </Match>
      </Switch>
    </div>
  )
}