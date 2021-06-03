/*
 * Copyright (c) 2021 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import PropTypes from 'prop-types'
import Head from 'next/head'
import { Provider } from 'react-redux'
import { useStore } from '../redux'
import '../index.scss'
import '@patternfly/react-core/dist/styles/base.css'
import { AuthProvider, useAuth } from '../auth'
import * as Sentry from '@sentry/react'
import { Integrations } from '@sentry/tracing'
import { QueryClient, QueryClientProvider } from 'react-query'
import { useMemo } from 'react'
import { configureProjectClient } from '../data/project'
import { configureExperimentClient } from '../data/experiments'
import { configureTopologyClient } from '../data/topology'

// This setup is necessary to forward the Auth0 context to the Redux context
const Inner = ({ Component, pageProps }) => {
    const auth = useAuth()

    const queryClient = useMemo(() => {
        const client = new QueryClient()
        configureProjectClient(client, auth)
        configureExperimentClient(client, auth)
        configureTopologyClient(client, auth)
        return client
    }, []) // eslint-disable-line react-hooks/exhaustive-deps
    const store = useStore(pageProps.initialReduxState, { auth, queryClient })
    return (
        <QueryClientProvider client={queryClient}>
            <Provider store={store}>
                <Component {...pageProps} />
            </Provider>
        </QueryClientProvider>
    )
}

Inner.propTypes = {
    Component: PropTypes.func,
    pageProps: PropTypes.shape({
        initialReduxState: PropTypes.object,
    }).isRequired,
}

const dsn = process.env.NEXT_PUBLIC_SENTRY_DSN
// Initialize Sentry if the user has configured a DSN
if (process.browser && dsn) {
    if (dsn) {
        Sentry.init({
            environment: process.env.NODE_ENV,
            dsn: dsn,
            integrations: [new Integrations.BrowserTracing()],
            tracesSampleRate: 0.1,
        })
    }
}

export default function App(props) {
    return (
        <>
            <Head>
                <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />
                <meta name="theme-color" content="#00A6D6" />
            </Head>
            <Sentry.ErrorBoundary fallback={'An error has occurred'}>
                <AuthProvider>
                    <Inner {...props} />
                </AuthProvider>
            </Sentry.ErrorBoundary>
        </>
    )
}
