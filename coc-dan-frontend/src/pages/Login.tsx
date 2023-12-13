import { createSignal } from "solid-js"
import "./Login/style.css"
import { User, setGlobalUser } from "../core/user";
import { post } from "../core";
import toast from "solid-toast";
import { try_login } from "../core/user/login";
import { IUser } from "../bindings/IUser";
import { useNavigate } from "@solidjs/router";

export default () => {
  const [username, setUsername] = createSignal<string>("");
  const handleInputChange = (e : any) => {
    setUsername(e.currentTarget.value)
  }

  const [loginButtonState, setLoginButtonState] = createSignal<string>("")
  async function toggleLogin() {
    setLoginButtonState("is-loading")
    try {
      const user : IUser = await post("/api/user/login", { name : username()})
      setLoginButtonState("is-ok")
      toast.success("login success")
      afterLogin(new User(user))
    } catch (error : any) {
      setLoginButtonState("is-danger")
      return
    }
  }

  const navigate = useNavigate()

  try_login().then((u : User | null) => {
    if (u != null) {
      afterLogin(u)
    }
  })

  function afterLogin(u : User) {
    console.log("login success ", u)
    setGlobalUser(u)
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
              onInput={handleInputChange}
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
