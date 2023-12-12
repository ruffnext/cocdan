/* @refresh reload */
import { lazy } from 'solid-js'
import { render } from 'solid-js/web'
import { Router } from '@solidjs/router'

const root = document.getElementById('root')

const routes = [
    {
        path: '/login',
        component: lazy(() => import('./pages/Login'))
    },
    {
        path: '/',
        component: lazy(() => import('./pages/Index'))
    }
]

render(() => 
    <Router>{routes}</Router>, 
root!)
