import "../node_modules/bulma/css/bulma.min.css";
import { Route, Router } from '@solidjs/router';
import { lazy } from 'solid-js';
import { render } from 'solid-js/web';
import { Toaster } from "solid-toast";

const Index = lazy(() => import('./pages/Index'))
const Login = lazy(() => import('./pages/Login'))

render(() =>
  <>
    <Router>
      <Route path="/" component={Index}></Route>
      <Route path="/login" component={Login}></Route>
    </Router>
    <Toaster />
  </>,
  document.getElementById('root')!)
