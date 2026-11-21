"use client"

import { UserDrawer } from "@/components/user/UserDrawer"
import { AVATAR_URL } from "@/components/user/UserIdentity"
import { useAuth } from "@/lib/auth/auth"
import { Avatar, Button, Skeleton, Text } from "@mantine/core"
import { useDisclosure } from "@mantine/hooks"

export function UserMenu() {
    const auth = useAuth()
    const [opened, { open, close }] = useDisclosure(false)

    if (auth.status === "loading") {
        return <Skeleton height={32} width={130} radius="lg" />
    }
    // AuthGate explains every other state in the main area.
    if (auth.status !== "signedIn") {
        return undefined
    }

    const { userName } = auth.session
    return (
        <>
            <UserDrawer opened={opened} close={close} session={auth.session} />
            <Button
                variant="subtle"
                color="gray"
                radius="lg"
                c="white"
                aria-label="Open account menu"
                leftSection={<Avatar src={AVATAR_URL} alt={userName} size="sm" radius="xl" />}
                onClick={open}
            >
                <Text size="sm" visibleFrom="sm">
                    {userName}
                </Text>
            </Button>
        </>
    )
}
