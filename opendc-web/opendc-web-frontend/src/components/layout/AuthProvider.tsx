"use client"

import { Auth0Session } from "@/components/layout/Auth0Session"
import { MessagePage } from "@/components/layout/MessagePage"
import { PageLoader } from "@/components/layout/PageLoader"
import { UNREACHABLE_TITLE, problemOf } from "@/lib/api/client"
import { useDeploymentConfig } from "@/lib/api/config"
import { Button } from "@mantine/core"
import { IconRefresh } from "@tabler/icons-react"
import type { ReactNode } from "react"

/**
 * Sets up sign-in as the server's runtime config says, so one build serves every deployment.
 * Children wait for it, since every request has to know whether it carries a token.
 */
export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
    const config = useDeploymentConfig()

    if (config.isPending) return <PageLoader />
    if (config.isError) {
        return (
            <MessagePage title={UNREACHABLE_TITLE} message={problemOf(config.error).title}>
                <Button leftSection={<IconRefresh size={16} />} onClick={() => void config.refetch()}>
                    Try again
                </Button>
            </MessagePage>
        )
    }
    const auth = config.data.auth
    switch (auth.type) {
        case "anonymous":
            return children
        case "auth0":
            return <Auth0Session settings={auth}>{children}</Auth0Session>
    }
}
