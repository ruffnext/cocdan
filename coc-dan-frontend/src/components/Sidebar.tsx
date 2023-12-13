import "./Sidebar/style.css"

const SIDE_BAR_WIDTH = "60px"

function Sidebar () {
  return <nav class="main-menu">
    <div id="sidebar-title">TITLE</div>
    <ul>
      <li>
        <a href="/home">
          <i class="fa fa-home fa-2x"></i>
          <span class="nav-text">
            Home
          </span>
        </a>
      </li>
      
      <li class="has-subnav">
        <a href="#">
          <i class="fa fa-person-walking-dashed-line-arrow-right fa-2x"></i>
          <span class="nav-text">
            My Avatars
          </span>
        </a>
      </li>
      
      <li class="has-subnav">
        <a href="#">
          <i class="fa fa-pen-nib fa-2x"></i>
          <span class="nav-text">
            Stages
          </span>
        </a>
      </li>
      
    </ul>

    <ul class="logout">
      <li>
        <a href="#">
          <i class="fa fa-power-off fa-2x"></i>
          <span class="nav-text">
            Logout
          </span>
        </a>
      </li>
    </ul>
  </nav>
}

export function SidebarWrapper(props : any) {
  return (
    <div>
      <div style={{ "margin-left" : SIDE_BAR_WIDTH }}>
        {props.children}
      </div>
      <Sidebar />
    </div>
  );
}
