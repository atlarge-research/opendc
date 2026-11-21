"use client"

import { formatDuration, formatMemory, formatUpdatedAt } from "@/components/format"
import { phaseBadge, phaseSince, scenarioRanges } from "@/lib/admin/phase"
import type { AdminExecution } from "@/lib/api/admin"
import { Badge, Paper, Stack, Table, Text } from "@mantine/core"

export function ExecutionTable({
    executions,
    onOpen,
}: Readonly<{ executions: AdminExecution[]; onOpen: (id: string) => void }>) {
    if (executions.length === 0) {
        return (
            <Paper withBorder radius="md" p="md">
                <Text size="sm" c="dimmed">
                    No executions in this state.
                </Text>
            </Paper>
        )
    }

    return (
        <Paper withBorder radius="md" p={0}>
            <Table highlightOnHover verticalSpacing="sm" horizontalSpacing="md">
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Experiment</Table.Th>
                        <Table.Th w={120}>Phase</Table.Th>
                        <Table.Th visibleFrom="sm">Scenarios</Table.Th>
                        <Table.Th visibleFrom="md">Grant</Table.Th>
                        <Table.Th visibleFrom="sm">Since</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {executions.map((execution) => (
                        <ExecutionRow key={execution.id} execution={execution} onOpen={onOpen} />
                    ))}
                </Table.Tbody>
            </Table>
        </Paper>
    )
}

function ExecutionRow({ execution, onOpen }: Readonly<{ execution: AdminExecution; onOpen: (id: string) => void }>) {
    const badge = phaseBadge(execution.phase)
    return (
        <Table.Tr style={{ cursor: "pointer" }} onClick={() => onOpen(execution.id)}>
            <Table.Td>
                <Stack gap={0}>
                    <Text size="sm" fw={500} truncate>
                        {execution.experimentName}
                    </Text>
                    <Text size="xs" c="dimmed" truncate>
                        {execution.projectName}
                        {execution.owner !== "" && ` - @${execution.owner}`}
                        {execution.attempt > 1 && ` - attempt ${execution.attempt}`}
                    </Text>
                </Stack>
            </Table.Td>
            <Table.Td>
                <Badge color={badge.color} variant="light">
                    {badge.label}
                </Badge>
            </Table.Td>
            <Table.Td visibleFrom="sm">
                <Text size="sm">{scenarioRanges(execution.scenarios)}</Text>
                <Text size="xs" c="dimmed">
                    {execution.unitCount} units
                </Text>
            </Table.Td>
            <Table.Td visibleFrom="md">
                <Text size="sm">
                    {execution.cores} cores, {formatMemory(execution.memoryRequestMb)}
                </Text>
                <Text size="xs" c="dimmed">
                    est. {formatDuration(execution.estimatedMakespanSeconds)} of{" "}
                    {formatDuration(execution.timeLimitSeconds)}
                </Text>
            </Table.Td>
            <Table.Td visibleFrom="sm">
                <Text size="sm">{formatUpdatedAt(phaseSince(execution.phase, execution.createdAt))}</Text>
            </Table.Td>
        </Table.Tr>
    )
}
