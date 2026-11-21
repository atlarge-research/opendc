"use client"

import { Box, AppShell as MantineAppShell } from "@mantine/core"
import type { ReactNode } from "react"
import { AppFooter } from "./AppFooter"
import { AppHeader } from "./AppHeader"
import { AuthGate } from "./AuthGate"

const HEADER_HEIGHT = 56

export function AppShell({ children }: { children: ReactNode }) {
    return (
        <MantineAppShell header={{ height: HEADER_HEIGHT }} withBorder={false}>
            <MantineAppShell.Header>
                <AppHeader />
            </MantineAppShell.Header>
            <MantineAppShell.Main
                bg="light-dark(var(--mantine-color-gray-1), var(--mantine-color-dark-8))"
                style={{ display: "flex", flexDirection: "column" }}
            >
                <Box flex={1}>
                    <AuthGate>{children}</AuthGate>
                </Box>
                <AppFooter />
            </MantineAppShell.Main>
        </MantineAppShell>
    )
}
