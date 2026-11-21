"use client"

import { formatDateTime } from "@/components/format"
import { TableGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { ADMIN_PAGE_SIZE, type AdminAccount, useAdminAccounts } from "@/lib/api/admin"
import { Badge, Group, Pagination, Paper, Stack, Table, Text, TextInput } from "@mantine/core"
import { useDebouncedValue } from "@mantine/hooks"
import { IconSearch } from "@tabler/icons-react"
import { useState } from "react"

const SEARCH_DEBOUNCE_MS = 300

export function AccountsPanel() {
    const [search, setSearch] = useState("")
    const [query] = useDebouncedValue(search, SEARCH_DEBOUNCE_MS)
    const [page, setPage] = useState(1)
    const accounts = useAdminAccounts(query, page)
    const pages = Math.max(1, Math.ceil((accounts.data?.total ?? 0) / ADMIN_PAGE_SIZE))

    return (
        <Stack gap="md">
            <Group justify="space-between">
                <TextInput
                    aria-label="Search accounts"
                    placeholder="Handle or name"
                    leftSection={<IconSearch size={16} />}
                    value={search}
                    w={260}
                    onChange={(event) => {
                        setSearch(event.currentTarget.value)
                        setPage(1)
                    }}
                />
                {pages > 1 && <Pagination total={pages} value={page} onChange={setPage} size="sm" />}
            </Group>
            <QueryState query={accounts} ghost={<TableGhost columns={4} rows={4} />}>
                {(loaded) => <AccountTable accounts={loaded.items} />}
            </QueryState>
        </Stack>
    )
}

function AccountTable({ accounts }: Readonly<{ accounts: AdminAccount[] }>) {
    if (accounts.length === 0) {
        return (
            <Paper withBorder radius="md" p="md">
                <Text size="sm" c="dimmed">
                    No account matches.
                </Text>
            </Paper>
        )
    }
    return (
        <Paper withBorder radius="md" p={0}>
            <Table verticalSpacing="sm" horizontalSpacing="md">
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Account</Table.Th>
                        <Table.Th w={110}>Plan</Table.Th>
                        <Table.Th visibleFrom="sm">Projects</Table.Th>
                        <Table.Th visibleFrom="sm">Joined</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {accounts.map((account) => (
                        <AccountRow key={account.subject} account={account} />
                    ))}
                </Table.Tbody>
            </Table>
        </Paper>
    )
}

function AccountRow({ account }: Readonly<{ account: AdminAccount }>) {
    const handle = account.handle.type === "chosen" ? `@${account.handle.name}` : "no handle yet"
    return (
        <Table.Tr>
            <Table.Td>
                <Group gap="xs" wrap="nowrap">
                    <Stack gap={0}>
                        <Text size="sm" fw={500}>
                            {account.displayName}
                        </Text>
                        <Text size="xs" c="dimmed">
                            {handle} - {account.subject}
                        </Text>
                    </Stack>
                    {account.isAdmin && (
                        <Badge size="xs" color="red" variant="light">
                            Admin
                        </Badge>
                    )}
                    {account.status.type === "deactivated" && (
                        <Badge size="xs" color="gray" variant="light">
                            Deactivated
                        </Badge>
                    )}
                </Group>
            </Table.Td>
            <Table.Td>
                <Text size="sm" tt="capitalize">
                    {account.plan}
                </Text>
            </Table.Td>
            <Table.Td visibleFrom="sm">{account.projectCount}</Table.Td>
            <Table.Td visibleFrom="sm">{formatDateTime(account.createdAt)}</Table.Td>
        </Table.Tr>
    )
}
