"use client"

import { AccountActions } from "@/components/user/AccountActions"
import { SimulationBudget } from "@/components/user/SimulationBudget"
import { UserIdentity } from "@/components/user/UserIdentity"
import type { AuthSession } from "@/lib/auth/auth"
import { Divider, Drawer, NavLink, Stack, useComputedColorScheme, useMantineColorScheme } from "@mantine/core"
import { IconMoon, IconSun } from "@tabler/icons-react"

export function UserDrawer({
    opened,
    close,
    session,
}: Readonly<{ opened: boolean; close: () => void; session: AuthSession }>) {
    return (
        <Drawer opened={opened} onClose={close} position="right" size="sm" title="Account" keepMounted>
            <Stack gap="md">
                <UserIdentity session={session} />
                <Divider />
                <SimulationBudget budgets={session.account.budgets} />
                <Divider />
                <Stack gap={0}>
                    <ColorSchemeItem />
                    <AccountActions session={session} closeDrawer={close} />
                </Stack>
            </Stack>
        </Drawer>
    )
}

function ColorSchemeItem() {
    const { setColorScheme } = useMantineColorScheme()
    const computed = useComputedColorScheme("light", { getInitialValueInEffect: true })
    const isDark = computed === "dark"
    return (
        <NavLink
            component="button"
            label={isDark ? "Light mode" : "Dark mode"}
            leftSection={isDark ? <IconSun size={16} /> : <IconMoon size={16} />}
            onClick={() => setColorScheme(isDark ? "light" : "dark")}
            aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
        />
    )
}
