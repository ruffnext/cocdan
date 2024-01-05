import "../node_modules/bulma/css/bulma.min.css";
import "./assets/app.css"
import { Route, Router } from '@solidjs/router';
import { lazy } from 'solid-js';
import { render } from 'solid-js/web';
import { Toaster } from "solid-toast";
import { SidebarWrapper } from "./components/Sidebar";
import Home from "./pages/Home/Page";
import { SupportedI18N, I18nProvider } from "./core/i18n";
import NewStage from "./pages/Stage/NewStage";

const Login = lazy(() => import('./pages/Login/Page'))
const Index = lazy(() => import('./pages/Index'))
const Avatar = lazy(() => import('./pages/Avatar'))
const Card = lazy(() => import('./pages/Card/Page'))

render(() =>
  <I18nProvider i18n = { SupportedI18N.zh_CN } >
    <Router>
      <Route path="/" component={Index}></Route>
      <Route path="/" component={SidebarWrapper}>
        <Route path="/home" component={Home}></Route>
        <Route path="/card/:id" component={Card}></Route>
        <Route path="/stage/new" component={NewStage}></Route>
      </Route>
      <Route path="/login" component={Login}></Route>
      <Route path="/avatar" component={Avatar} />
    </Router>
    <Toaster />
  </I18nProvider>,
  document.getElementById('root')!)
