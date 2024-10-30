import { createSignal } from "solid-js"
import "./style.css"
import toast from "solid-toast";
import { useNavigate } from "@solidjs/router";
import { useSession } from "./context";
import { ISession } from "../../bindings/entity/basic/ISession";
import { post } from "../../api/core";

export default () => {
  const { session, setSession } = useSession()
  const [username, setUsername] = createSignal<string>("");
  const [password, setPassword] = createSignal<string>("");
  const handleUsernameChange = (e: any) => {
    setUsername(e.currentTarget.value)
  }
  const handlePasswordChange = (e: any) => {
    setPassword(e.currentTarget.value)
  }

  const [loginButtonState, setLoginButtonState] = createSignal<string>("")
  async function toggleLogin() {
    setLoginButtonState("is-loading")
    const ret = await post('/user/login', { username: username(), password: password() }, undefined)
    if ("Ok" in ret) {
      setLoginButtonState("is-ok")
      toast.success("login success")
      afterLogin(ret.Ok)
    } else {
      setLoginButtonState("is-danger")
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

  return <div id="login-background">
    <div id="login-card">
      <img id='login-header' src="/img/soslogo.jpg"></img>
      <div id="login-main">
        <div class="field">
          <p class="control has-icons-left">
            <input
              class="input"
              type="text"
              placeholder="Username"
              value={username()}
              onInput={handleUsernameChange}
            />
            <span class="icon is-small is-left">
              <i class="fas fa-user"></i>
            </span>
          </p>
        </div>
        <div class="field">
          <p class="control has-icons-left">
            <input
              class="input"
              type="password"
              placeholder="Password"
              value={password()}
              onInput={handlePasswordChange}
            />
            <span class="icon is-small is-left">
              <i class="fas fa-user"></i>
            </span>
          </p>
        </div>
        <button
          id="login-button"
          class={"button is-primary " + loginButtonState()}
          onClick={toggleLogin}>
          Login
        </button>
      </div>
    </div>
  </div>
}
