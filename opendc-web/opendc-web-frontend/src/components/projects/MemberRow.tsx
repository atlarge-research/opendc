"use client"

import type { ProjectMember } from "@/lib/api/members"
import type { ProjectRole } from "@/lib/api/types"
import { ActionIcon, Badge, Group, Select, Stack, Text } from "@mantine/core"
import { IconUserMinus } from "@tabler/icons-react"

export const ROLE_OPTIONS: { value: ProjectRole; label: string }[] = [
    { value: "owner", label: "Owner" },
    { value: "editor", label: "Editor" },
    { value: "viewer", label: "Viewer" },
]

export const ROLE_SELECT_WIDTH = 110

export function MemberRow({
    member,
    manageable,
    onRoleChange,
    onRemove,
}: Readonly<{
    member: ProjectMember
    manageable: boolean
    onRoleChange: (role: ProjectRole) => void
    onRemove: () => void
}>) {
    return (
        <Group justify="space-between" wrap="nowrap">
            <Stack gap={0}>
                <Text size="sm" fw={500}>
                    {member.displayName}
                </Text>
                <Text size="xs" c="dimmed">
                    @{member.handle}
                </Text>
            </Stack>
            {manageable ? (
                <Group gap="xs" wrap="nowrap">
                    <Select
                        size="xs"
                        w={ROLE_SELECT_WIDTH}
                        data={ROLE_OPTIONS}
                        value={member.role}
                        allowDeselect={false}
                        aria-label={`Role of ${member.handle}`}
                        onChange={(role) => role && onRoleChange(role as ProjectRole)}
                    />
                    <ActionIcon variant="subtle" color="red" aria-label={`Remove ${member.handle}`} onClick={onRemove}>
                        <IconUserMinus size={16} />
                    </ActionIcon>
                </Group>
            ) : (
                <Badge variant="light" color="gray">
                    {member.role}
                </Badge>
            )}
        </Group>
    )
}
