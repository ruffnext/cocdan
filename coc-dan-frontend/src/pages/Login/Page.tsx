import { createSignal } from "solid-js"
import "./style.css"
import { User } from "../../core/user";
import { post } from "../../core";
import toast from "solid-toast";
import { IUser } from "../../bindings/IUser";
import { useNavigate } from "@solidjs/router";
import { IUserLogin } from "../../bindings/user/service/IUserLogin";
import { useUser } from "./context";

export default () => {
  const { user, setUser } = useUser()
  const [username, setUsername] = createSignal<string>("");
  const handleInputChange = (e : any) => {
    setUsername(e.currentTarget.value)
  }

  const [loginButtonState, setLoginButtonState] = createSignal<string>("")
  async function toggleLogin() {
    setLoginButtonState("is-loading")
    const params : IUserLogin = {name : username()}
    try {
      const user : IUser = await post("/api/user/login", params)
      setLoginButtonState("is-ok")
      toast.success("login success")
      afterLogin(new User(user))
    } catch (error : any) {
      setLoginButtonState("is-danger")
      return
    }
  }

  const navigate = useNavigate()

  if (user() != undefined) {
    navigate("/home")
  }

  function afterLogin(u : User) {
    console.log("login success ", u)
    setUser(u)
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
