import { post } from "../../../../../api/core"
import "./style.scss"
import { useNavigate } from "@solidjs/router"
import { useSession } from "../../../../Login/context"
import { createSignal } from "solid-js"

export function SidebarWrapper(props: any) {
  const [isExtend, setIsExtend] = createSignal(true)
  const navigate = useNavigate()
  const { setSession } = useSession()
  async function logout() {
    await post('/user/logout', undefined, undefined, false)
    setSession("NotLoggedIn")
    navigate("/login")
  }
  return (
    <main class={`bg-bg w-full h-full max-w-100vw text-textcolor flex`}>
      <button class={`absolute top-1 left-1 h-8 w-16 ${isExtend() ? "hidden" : ""} justify-center items-center flex bg-green-300 rounded-md`} on:click={() => setIsExtend(true)}>
        <svg class="h-8 w-8 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3" />
        </svg>
      </button>
      <div class={`h-full w-20 min-w-20 flex-col items-center bg-bgcolor text-textcolor shadow-lg relative rs-sidebar ${isExtend() ? "flex" : "hidden"}`}>
        <button class="flex h-8 w-16 hover:bg-green-300 rounded-md items-center justify-center mt-1 mb-1" on:click={() => setIsExtend(false)}>
          <svg class="h-8 w-8 text-red-500" width="24" height="24"
            viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none"
            stroke-linecap="round" stroke-linejoin="round">
            <path stroke="none" d="M0 0h24v24H0z" />
            <line x1="5" y1="12" x2="19" y2="12" />
            <line x1="5" y1="12" x2="9" y2="16" />
            <line x1="5" y1="12" x2="9" y2="8" /></svg>
        </button>
        <hr class="w-full" />
        <div class="flex items-center px-2 flex-col flex-1">
          <button class="w-16 h-16 bg-red-500 mt-2 mb-2">
          </button>
          <button class="w-16 h-16 bg-red-500 mt-2 mb-2">
          </button>
          <button class="w-16 h-16 bg-red-500 mt-2 mb-2">
          </button>
          <div class="flex-1"></div>
          <button class="w-12 h-12 mt-2 mb-2 text-black-500 hover:text-red-500">
            <svg class="h-12 w-12"
              width="24" height="24" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none"
              stroke-linecap="round" stroke-linejoin="round">
              <path stroke="none" d="M0 0h24v24H0z" />
              <path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 0 0 2.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 0 0 1.065 
                        2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 0 0 -1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 0 0 -2.572 
                        1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 0 0 -2.573 -1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 0 0 -1.065 
                        -2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 0 0 1.066 -2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
          </button>
          <button class="w-12 h-12 mt-2 mb-2 text-black hover:text-red-500" on:click={() => logout()}>
            <svg class="h-12 w-12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" stroke-linejoin="round">  <path d="M18.36 6.64a9 9 0 1 1-12.73 0" />
              <line x1="12" y1="2" x2="12" y2="12" />
            </svg>
          </button>
          <div class="h-4 w-full"></div>
        </div>
      </div>
      <div class="flex-grow h-full">
        {props.children}
      </div>
    </main>
  );
}
