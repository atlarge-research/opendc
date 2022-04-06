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
import { auth } from './config'

/**
 * Helper function to provide the authentication context in case Auth0 is not
 * configured.
 */
function useAuthDev() {
    return {
        isAuthenticated: false,
        isLoading: false,
        logout: () => {},
        loginWithRedirect: () => {},
    }
}

/**
 * Obtain the authentication context.
 */
export const useAuth = auth.domain ? useAuth0 : useAuthDev

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
    if (auth.domain) {
        return (
            <Auth0Provider
                domain={auth.domain}
                clientId={auth.clientId}
                redirectUri={auth.redirectUri}
                audience={auth.audience}
            >
                {children}
            </Auth0Provider>
        )
    }

    return children
}

AuthProvider.propTypes = {
    children: PropTypes.node,
}
