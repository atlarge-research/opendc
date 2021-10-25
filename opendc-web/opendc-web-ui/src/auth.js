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
import { Auth0Provider, useAuth0 } from '@auth0/auth0-react'
import { useEffect } from 'react'

/**
 * Obtain the authentication context.
 */
export function useAuth() {
    return useAuth0()
}

/**
 * Force the user to be authenticated or redirect to the homepage.
 */
export function useRequireAuth() {
    const auth = useAuth()
    const { loginWithRedirect, isLoading, isAuthenticated } = auth

    useEffect(() => {
        if (!isLoading && !isAuthenticated) {
            loginWithRedirect()
        }
    }, [loginWithRedirect, isLoading, isAuthenticated])

    return auth
}

/**
 * AuthProvider which provides an authentication context.
 */
export function AuthProvider({ children }) {
    return (
        <Auth0Provider
            domain={process.env.NEXT_PUBLIC_AUTH0_DOMAIN}
            clientId={process.env.NEXT_PUBLIC_AUTH0_CLIENT_ID}
            redirectUri={global.window && global.window.location.origin}
            audience={process.env.NEXT_PUBLIC_AUTH0_AUDIENCE}
        >
            {children}
        </Auth0Provider>
    )
}

AuthProvider.propTypes = {
    children: PropTypes.node,
}
