import { createSignal } from "solid-js"
import toast from "solid-toast";
import { useNavigate } from "@solidjs/router";
import { useSession } from "./context";
import { ISession } from "../../bindings/entity/basic/ISession";
import { post } from "../../api/core";

export default () => {
  const { session, setSession } = useSession()
  const [username, setUsername] = createSignal<string>("");
  const [password, setPassword] = createSignal<string>("");

  async function toggleLogin() {
    const ret = await post('/user/login', { username: username(), password: password() }, undefined)
    if ("Ok" in ret) {
      toast.success("login success")
      afterLogin(ret.Ok)
    } else {
      return
    }
  }

  const navigate = useNavigate()

  if (session() != "NotLoggedIn" && session() != "IsLoading") {
    navigate("/home")
  }

  function afterLogin(u: ISession) {
    setSession(u)
    navigate("/home")
  }

  return (
    <div class="w-full h-full bg-gray-100">
      <div class="h-full content-center max-w-xs m-auto ">
        <div class="bg-white shadow-md rounded px-8 pt-6 mb-6 pb-8">
          <div class="w-full mb-4 mt-4">
            <img class="ml-auto mr-auto" src="/img/soslogo.jpg" alt="" />
          </div>
          <div class="mb-4">
            <label class="block text-gray-700 text-sm font-bold mb-2" for="username">
              Username
            </label>
            <input class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              id="username" type="text" placeholder="Username" on:change={(e) => setUsername(e.target.value)} />
          </div>
          <div class="mb-4">
            <label class="block text-gray-700 text-sm font-bold mb-2" for="password">
              Password
            </label>
            <input class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              id="password" type="password" placeholder="Password" on:change={(e) => setPassword(e.target.value)} />
          </div>
          <button class="w-full h-10 mt-4 rounded-md bg-green-300 hover:bg-green-500" on:click={() => toggleLogin()}>Login</button>
        </div>
      </div>
    </div>
  )
}
