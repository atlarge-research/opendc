"use client"

import { MessagePage } from "@/components/layout/MessagePage"
import { OnboardingPage } from "@/components/user/OnboardingPage"
import { UNREACHABLE_TITLE } from "@/lib/api/client"
import { useAuth } from "@/lib/auth/auth"
import { useSessionControls } from "@/lib/auth/session"
import { Button } from "@mantine/core"
import { IconLogin, IconLogout, IconRefresh } from "@tabler/icons-react"
import type { ReactNode } from "react"

/**
 * Replaces the page when nobody is signed in, the account is deactivated or has no handle yet, or the
 * API is unreachable. While loading the page renders anyway, so requests are not queued behind the
 * session; the server, not this gate, is what guards the data.
 */
export function AuthGate({ children }: Readonly<{ children: ReactNode }>) {
    const auth = useAuth()
    const controls = useSessionControls()

    switch (auth.status) {
        case "unavailable":
            return (
                <MessagePage title={UNREACHABLE_TITLE} message={auth.problem.title}>
                    <Button leftSection={<IconRefresh size={16} />} onClick={auth.retry}>
                        Try again
                    </Button>
                </MessagePage>
            )
        case "signedOut":
            return (
                <MessagePage
                    title="Sign in to OpenDC"
                    message="Your projects, topologies and experiments live in your account."
                >
                    {controls.type === "auth0" && (
                        <Button leftSection={<IconLogin size={16} />} onClick={controls.signIn}>
                            Sign in
                        </Button>
                    )}
                </MessagePage>
            )
        case "deactivated":
            return (
                <MessagePage
                    title="This account has been deactivated"
                    message="It can no longer use OpenDC. Projects you shared with others stay with them."
                >
                    {controls.type === "auth0" && (
                        <Button leftSection={<IconLogout size={16} />} variant="default" onClick={controls.signOut}>
                            Sign out
                        </Button>
                    )}
                </MessagePage>
            )
        case "needsHandle":
            return <OnboardingPage />
        case "loading":
        case "signedIn":
            return children
    }
}
