import React from 'react'
import ReactDOM from 'react-dom'
import * as Sentry from '@sentry/react'
import { Integrations } from '@sentry/tracing'
import { Provider } from 'react-redux'
import { setupSocketConnection } from './api/socket'
import './index.scss'
import Routes from './routes'
import config from './config'
import configureStore from './store/configure-store'

setupSocketConnection(() => {
    const store = configureStore()

    // Initialize Sentry if the user has configured a DSN
    const dsn = config['SENTRY_DSN']
    if (dsn) {
        Sentry.init({
            environment: process.env.NODE_ENV,
            dsn: dsn,
            integrations: [new Integrations.BrowserTracing()],
            tracesSampleRate: 0.1,
        })
    }

    ReactDOM.render(
        <Provider store={store}>
            <Routes />
        </Provider>,
        document.getElementById('root')
    )
})
