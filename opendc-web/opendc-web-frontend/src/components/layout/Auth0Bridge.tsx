"use client"

import { PageLoader } from "@/components/layout/PageLoader"
import { meKeys } from "@/lib/api/account"
import { setAuthHeaders } from "@/lib/api/client"
import { type SessionControls, SessionProvider } from "@/lib/auth/session"
import { config } from "@/lib/config"
import { useAuth0 } from "@auth0/auth0-react"
import { useQueryClient } from "@tanstack/react-query"
import { type ReactNode, useEffect, useState } from "react"

/**
 * Wires Auth0 access tokens into the API client and provides the sign-in controls. Children wait for
 * the wiring, or their first requests would go out without a token and read as signed out.
 */
export function Auth0Bridge({ children }: Readonly<{ children: ReactNode }>) {
    const { isLoading, isAuthenticated, getAccessTokenSilently, loginWithRedirect, logout, user } = useAuth0()
    const queryClient = useQueryClient()
    const [wired, setWired] = useState(false)

    useEffect(() => {
        if (isLoading) return
        setAuthHeaders(
            isAuthenticated
                ? async () => ({ Authorization: `Bearer ${await getAccessTokenSilently()}` })
                : async () => ({}),
        )
        setWired(true)
        void queryClient.invalidateQueries({ queryKey: meKeys.profile })
    }, [isLoading, isAuthenticated, getAccessTokenSilently, queryClient])

    if (!wired) return <PageLoader />

    const controls: SessionControls = {
        type: "auth0",
        signIn: () =>
            void loginWithRedirect({ appState: { returnTo: window.location.pathname + window.location.search } }),
        signOut: () => {
            queryClient.clear()
            void logout({ logoutParams: { returnTo: `${window.location.origin}${config.basePath}/` } })
        },
        hint: { nickname: user?.nickname ?? "", name: user?.name ?? "" },
    }
    return <SessionProvider value={controls}>{children}</SessionProvider>
}
