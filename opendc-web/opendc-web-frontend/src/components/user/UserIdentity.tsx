"use client"

import type { Account, Handle } from "@/lib/api/types"
import type { AuthSession } from "@/lib/auth/auth"
import { Avatar, Badge, Group, Stack, Text } from "@mantine/core"
import { IconFolder, IconUser, IconUserKey } from "@tabler/icons-react"

export const AVATAR_URL = "/img/avatar.svg"

export function UserIdentity({ session }: Readonly<{ session: AuthSession }>) {
    return (
        <Stack gap="sm">
            <Group gap="sm" wrap="nowrap">
                <Avatar src={AVATAR_URL} alt={session.userName} size="md" radius="xl" />
                <Stack gap={0}>
                    <Text size="sm" fw={600}>
                        {session.userName}
                    </Text>
                    <Text size="xs" c="dimmed">
                        {handleLabel(session.handle)}
                    </Text>
                </Stack>
            </Group>
            <AccountBadges account={session.account} />
        </Stack>
    )
}

function handleLabel(handle: Handle): string {
    switch (handle.type) {
        case "provisional":
            return "No handle chosen yet"
        case "chosen":
            return `@${handle.name}`
    }
}

function AccountBadges({ account }: Readonly<{ account: Account }>) {
    return (
        <Group gap="xs">
            <Badge variant="light" color="opendc" size="sm" leftSection={<IconUser size={12} />}>
                {account.plan}
            </Badge>
            <Badge variant="light" color="gray" size="sm" leftSection={<IconFolder size={12} />}>
                {account.projectCount} projects
            </Badge>
            {account.isAdmin && (
                <Badge variant="light" color="red" size="sm" leftSection={<IconUserKey size={12} />}>
                    Admin
                </Badge>
            )}
        </Group>
    )
}
