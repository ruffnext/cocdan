import "./index.css"
import { Route, Router } from '@solidjs/router';
import { lazy } from 'solid-js';
import { render } from 'solid-js/web';
import { Toaster } from "solid-toast";
import Home from "./pages/Home/Page";
import { SupportedI18N, I18nProvider } from "./core/i18n";
import NewStage from "./pages/Stage/New/Component";
import { SessionProvider } from "./pages/Login/context";

const Login = lazy(() => import('./pages/Login/Page'))
const Index = lazy(() => import('./pages/Index'))
const Avatar = lazy(() => import('./pages/Avatar'))
const Card = lazy(() => import('./pages/Card/Page'))
const StagePerform = lazy(() => import('./pages/Stage/Perform/Component'))
const JoinStage = lazy(() => import('./pages/Stage/Join/Component'))

render(() =>
  <I18nProvider i18n={SupportedI18N.zh_CN} >
    <SessionProvider>
      <Router>
        <Route path="/" component={Index}></Route>
        <Route path="/home" component={Home}></Route>
        <Route path="/card/:id" component={Card}></Route>
        <Route path="/stage/new" component={NewStage}></Route>
        <Route path="/stage/:id/perform" component={StagePerform}></Route>
        <Route path="/stage/:id/join" component={JoinStage}></Route>
        <Route path="/login" component={Login}></Route>
        <Route path="/avatar" component={Avatar} />
      </Router>
      <Toaster />
    </SessionProvider>
  </I18nProvider>,
  document.getElementById('root')!)
