"use client"

import { AccountsPanel } from "@/components/admin/AccountsPanel"
import { CapacityPanel } from "@/components/admin/CapacityPanel"
import { ExecutionsPanel } from "@/components/admin/ExecutionsPanel"
import { useAuth } from "@/lib/auth/auth"
import { Container, Stack, Tabs, Text, Title } from "@mantine/core"

/** The operator's view. The gate around it has already settled that someone is signed in. */
export function AdminView() {
    const auth = useAuth()
    if (auth.status !== "signedIn") return undefined

    return (
        <Container size="lg" py="lg">
            <Stack gap="lg">
                <Title order={1} py={25}>
                    Administration
                </Title>
                {auth.session.account.isAdmin ? (
                    <Tabs defaultValue="executions" keepMounted={false}>
                        <Tabs.List mb="md">
                            <Tabs.Tab value="executions">Executions</Tabs.Tab>
                            <Tabs.Tab value="capacity">Capacity</Tabs.Tab>
                            <Tabs.Tab value="accounts">Accounts</Tabs.Tab>
                        </Tabs.List>
                        <Tabs.Panel value="executions">
                            <ExecutionsPanel />
                        </Tabs.Panel>
                        <Tabs.Panel value="capacity">
                            <CapacityPanel />
                        </Tabs.Panel>
                        <Tabs.Panel value="accounts">
                            <AccountsPanel />
                        </Tabs.Panel>
                    </Tabs>
                ) : (
                    <Text c="dimmed">Only administrators of this deployment can see this page.</Text>
                )}
            </Stack>
        </Container>
    )
}
