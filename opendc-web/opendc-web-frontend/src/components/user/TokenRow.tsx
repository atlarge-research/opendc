"use client"

import { formatUpdatedAt } from "@/components/format"
import type { AccessToken, TokenUse } from "@/lib/api/types"
import { ActionIcon, Table, Text } from "@mantine/core"
import { IconTrash } from "@tabler/icons-react"

/** One access token: what it is for, how it starts, and when it was last presented. */
export function TokenRow({ token, onRevoke }: Readonly<{ token: AccessToken; onRevoke: () => void }>) {
    return (
        <Table.Tr>
            <Table.Td>
                <Text size="sm" fw={500}>
                    {token.name}
                </Text>
                <Text size="xs" c="dimmed" ff="monospace">
                    {token.prefix}...
                </Text>
            </Table.Td>
            <Table.Td>
                <Text size="xs" c="dimmed">
                    {lastUseLabel(token.lastUse)}
                </Text>
            </Table.Td>
            <Table.Td w={40}>
                <ActionIcon variant="subtle" color="red" aria-label={`Revoke ${token.name}`} onClick={onRevoke}>
                    <IconTrash size={16} />
                </ActionIcon>
            </Table.Td>
        </Table.Tr>
    )
}

function lastUseLabel(use: TokenUse): string {
    switch (use.type) {
        case "unused":
            return "Never used"
        case "used":
            return `Used ${formatUpdatedAt(use.at)}`
    }
}
