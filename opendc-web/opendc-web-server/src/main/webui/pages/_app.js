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
import Script from 'next/script'
import { Provider } from 'react-redux'
import { useNewQueryClient } from '../data/query'
import { useStore } from '../redux'
import { AuthProvider, useRequireAuth } from '../auth'
import * as Sentry from '@sentry/react'
import { Integrations } from '@sentry/tracing'
import { QueryClientProvider } from 'react-query'
import { sentryDsn } from '../config'

import '@patternfly/react-core/dist/styles/base.css'
import '@patternfly/react-styles/css/utilities/Alignment/alignment.css'
import '@patternfly/react-styles/css/utilities/BackgroundColor/BackgroundColor.css'
import '@patternfly/react-styles/css/utilities/BoxShadow/box-shadow.css'
import '@patternfly/react-styles/css/utilities/Display/display.css'
import '@patternfly/react-styles/css/utilities/Flex/flex.css'
import '@patternfly/react-styles/css/utilities/Float/float.css'
import '@patternfly/react-styles/css/utilities/Sizing/sizing.css'
import '@patternfly/react-styles/css/utilities/Spacing/spacing.css'
import '@patternfly/react-styles/css/utilities/Text/text.css'
import '@patternfly/react-styles/css/components/InlineEdit/inline-edit.css'
import '../style/index.css'

// This setup is necessary to forward the Auth0 context to the Redux context
function Inner({ Component, pageProps }) {
    // Force user to be authorized
    useRequireAuth()

    const queryClient = useNewQueryClient()
    const store = useStore(pageProps.initialReduxState, { queryClient })
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

// Initialize Sentry if the user has configured a DSN
if (process.browser && sentryDsn) {
    Sentry.init({
        environment: process.env.NODE_ENV,
        dsn: sentryDsn,
        integrations: [new Integrations.BrowserTracing()],
        tracesSampleRate: 0.1,
    })
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
            {/* Google Analytics */}
            <Script async src="https://www.googletagmanager.com/gtag/js?id=UA-84285092-3" />
            <Script
                id="gtag"
                dangerouslySetInnerHTML={{
                    __html: `
                            window.dataLayer = window.dataLayer || [];
                            function gtag(){dataLayer.push(arguments);}
                            gtag('js', new Date());
                            gtag('config', 'UA-84285092-3');
                        `,
                }}
            />
        </>
    )
}
