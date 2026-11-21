"use client"

import { AuthProvider } from "@/components/layout/AuthProvider"
import { meKeys } from "@/lib/api/account"
import { ApiError } from "@/lib/api/client"
import { theme } from "@/theme/theme"
import { MantineProvider } from "@mantine/core"
import { ModalsProvider } from "@mantine/modals"
import { Notifications } from "@mantine/notifications"
import { QueryCache, QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { type ReactNode, useState } from "react"

export function Providers({ children }: { children: ReactNode }) {
    const [queryClient] = useState(() => {
        // A 401 anywhere means the session ended, so the profile that decides what is shown is refetched.
        const client: QueryClient = new QueryClient({
            defaultOptions: { queries: { refetchOnWindowFocus: false } },
            queryCache: new QueryCache({
                onError: (error, query) => {
                    const aboutCaller = query.queryKey[0] === meKeys.profile[0]
                    if (error instanceof ApiError && error.problem.status === 401 && !aboutCaller) {
                        void client.invalidateQueries({ queryKey: meKeys.profile })
                    }
                },
            }),
        })
        return client
    })
    return (
        <QueryClientProvider client={queryClient}>
            <MantineProvider theme={theme} defaultColorScheme="auto">
                <ModalsProvider>
                    <Notifications />
                    <AuthProvider>{children}</AuthProvider>
                </ModalsProvider>
            </MantineProvider>
        </QueryClientProvider>
    )
}
