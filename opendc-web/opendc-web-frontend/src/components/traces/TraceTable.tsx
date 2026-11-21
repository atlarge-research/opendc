"use client"

import { formatBytes, formatCount } from "@/components/format"
import { TraceActions } from "@/components/traces/TraceActions"
import type { Trace, TraceAccess } from "@/lib/api/types"
import { Badge, Group, Paper, Stack, Table, Text } from "@mantine/core"

// Built-in traces first: the same list for everybody, and the place to start.
const ACCESS_ORDER: Record<TraceAccess, number> = { builtin: 0, owned: 1, shared: 2 }

export function TraceTable({ traces }: { traces: Trace[] }) {
    if (traces.length === 0) {
        return (
            <Paper withBorder radius="md" p="md">
                <Text size="sm" c="dimmed">
                    Nothing here yet. Upload a trace, or ask a colleague to share one with you.
                </Text>
            </Paper>
        )
    }

    const ordered = [...traces].sort(
        (left, right) => ACCESS_ORDER[left.access] - ACCESS_ORDER[right.access] || left.slug.localeCompare(right.slug),
    )

    return (
        <Paper withBorder radius="md" p={0}>
            <Table highlightOnHover verticalSpacing="sm" horizontalSpacing="md" layout="fixed">
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Name</Table.Th>
                        {/* Narrow screens keep the name and the menu; what these hold moves under
                            the name, where it reads as a sentence rather than a squeezed column. */}
                        <Table.Th w={110} visibleFrom="sm">
                            Kind
                        </Table.Th>
                        <Table.Th w={110} visibleFrom="sm">
                            Size
                        </Table.Th>
                        <Table.Th w="30%" visibleFrom="md">
                            Rows
                        </Table.Th>
                        <Table.Th w={52} />
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {ordered.map((trace) => (
                        <Table.Tr key={trace.id}>
                            <Table.Td>
                                <TraceName trace={trace} />
                            </Table.Td>
                            <Table.Td visibleFrom="sm">
                                <Text size="sm">{trace.kind}</Text>
                            </Table.Td>
                            <Table.Td visibleFrom="sm">
                                <Text size="sm">{formatBytes(trace.sizeBytes)}</Text>
                            </Table.Td>
                            <Table.Td visibleFrom="md">
                                <TraceRows trace={trace} />
                            </Table.Td>
                            <Table.Td>
                                <TraceActions trace={trace} />
                            </Table.Td>
                        </Table.Tr>
                    ))}
                </Table.Tbody>
            </Table>
        </Paper>
    )
}

function TraceName({ trace }: { trace: Trace }) {
    return (
        <Stack gap={2}>
            <Group gap="xs" wrap="nowrap">
                <Text size="sm" fw={500} truncate>
                    {trace.slug}
                </Text>
                {trace.access !== "owned" && (
                    <Badge size="xs" variant="light" color={trace.access === "builtin" ? "gray" : "opendc"}>
                        {trace.access === "builtin" ? "Built in" : "Shared"}
                    </Badge>
                )}
            </Group>
            {trace.description && (
                <Text size="xs" c="dimmed" lineClamp={1}>
                    {trace.description}
                </Text>
            )}
            <Text size="xs" c="dimmed" hiddenFrom="sm">
                {trace.kind}, {formatBytes(trace.sizeBytes)}
            </Text>
            <Group gap="sm" hiddenFrom="md">
                <TraceRows trace={trace} />
            </Group>
        </Stack>
    )
}

// A trace is one or more tables, and their counts are different quantities rather than one to add
// up: tasks and fragments count different things.
function TraceRows({ trace }: { trace: Trace }) {
    const counted = trace.tables.filter((table) => table.rowCount !== undefined)
    if (counted.length === 0) {
        return (
            <Text size="xs" c="dimmed">
                -
            </Text>
        )
    }
    return (
        <Group gap="md" wrap="wrap">
            {counted.map((table) => (
                <Text key={table.name} size="xs" c="dimmed">
                    {formatCount(table.rowCount ?? 0)} {table.name}
                </Text>
            ))}
        </Group>
    )
}
