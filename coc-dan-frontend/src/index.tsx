import "../node_modules/bulma/css/bulma.min.css";
import "./assets/app.css"
import { Route, Router } from '@solidjs/router';
import { lazy } from 'solid-js';
import { render } from 'solid-js/web';
import { Toaster } from "solid-toast";
import { SidebarWrapper } from "./components/Sidebar";
import Home from "./pages/Home/Page";

const Login = lazy(() => import('./pages/Login/Page'))
const Index = lazy(() => import('./pages/Index'))
const Card = lazy(() => import('./pages/Card/Page'))

render(() =>
  <>
    <Router>
      <Route path="/" component={Index}></Route>
      <Route path="/" component={SidebarWrapper}>
        <Route path="/home" component={Home}></Route>
        <Route path="/card/:id" component={Card}></Route>
      </Route>
      <Route path="/login" component={Login}></Route>
    </Router>
    <Toaster />
  </>,
  document.getElementById('root')!)
