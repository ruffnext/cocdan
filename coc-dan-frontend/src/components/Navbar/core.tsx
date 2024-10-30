import { useNavigate } from "@solidjs/router";
import { useSession } from "../../pages/Login/context";
import "./style.css";
import { createSignal } from "solid-js";
import { Switch, Match } from "solid-js";
import { post } from "../../api/core";

function Menu() {
  const [isActive, setIsActive] = createSignal(false);
  const { session, setSession } = useSession();
  const navigate = useNavigate()

  const checkIsLoggedIn = () => {
    const v = session()
    console.log(v)
    if (v === "NotLoggedIn") {
      navigate("/login")
      return false
    } else if (v === "IsLoading") {
      return false
    } else {
      return true
    }
  }

  const toggle = () => {
    setIsActive(!isActive())
  };

  async function logout() {
    await post('/user/logout', undefined, undefined)
    setSession("NotLoggedIn")
    navigate("/login")
  }

  return (
    <div id="navbar-options" on:click={toggle}>
      <div class={`dropdown ${isActive() ? "is-active" : ""}`}>
        <div class="dropdown-menu" role="menu">
          <div class="dropdown-content">
            <a class="dropdown-item">
              <span class="icon-text">
                <span class="icon is-medium">
                  <i class="fas fa-user-pen fa-lg"></i>
                </span>
                <span class="navbar-menu-text">Avatars</span>
              </span>
            </a>
            <a class="dropdown-item" on:click={() => navigate("/stage/new")}>
              <span class="icon-text">
                <span class="icon is-medium">
                  <i class="fa-solid fa-book-open fa-lg"></i>
                </span>
                <span class="navbar-menu-text">Stage</span>
              </span>
            </a>
            <Switch>
              <Match when={checkIsLoggedIn()}>
                <a class="dropdown-item" on:click={logout}>
                  <span class="icon-text">
                    <span class="icon is-medium">
                      <i class="fa-solid fa-right-from-bracket fa-lg"></i>
                    </span>
                    <span class="navbar-menu-text">Logout</span>
                  </span>
                </a>
              </Match>
            </Switch>
          </div>
        </div>
      </div>
      <div id="navbar-click-listener" style={`display: ${isActive() ? "block" : "none"};`}></div>
    </div>
  )
}


function Navbar() {
  // const matches = useCurrentMatches();
  // const breadcrumbs = createMemo(() =>
  //   matches().map((m) => m.route.info.breadcrumb)
  // );

  return (
    <div id="navbar-container">
      <Menu />
      <div id="navbar-logo"></div>
      <div id="navbar-middle"></div>
      <div id="navbar-user"></div>
    </div>
  )
}


export function NavbarWrapper(props: any) {
  return (
    <div>
      <Navbar />
      <div style={{ "margin-top": "3em" }}>
        {props.children}
      </div>
    </div>
  );
}