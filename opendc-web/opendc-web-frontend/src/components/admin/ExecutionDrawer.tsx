"use client"

import { ExecutionLog } from "@/components/admin/ExecutionLog"
import { formatDuration, formatMemory } from "@/components/format"
import { LineGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { notifyProblem } from "@/components/util/feedback"
import { phaseBadge } from "@/lib/admin/phase"
import { type AdminExecutionDetail, useAdminExecution, useRetryExecution } from "@/lib/api/admin"
import { EXIT_REASON_LABELS, outcomeLabel } from "@/lib/experiment/outcomes"
import { Badge, Button, Drawer, Group, Stack, Table, Text } from "@mantine/core"
import { IconRefresh } from "@tabler/icons-react"

export function ExecutionDrawer({ id, onClose }: Readonly<{ id: string | undefined; onClose: () => void }>) {
    const detail = useAdminExecution(id)
    return (
        <Drawer opened={id !== undefined} onClose={onClose} position="right" size="xl" title="Execution">
            {id !== undefined && (
                <QueryState query={detail} ghost={<LineGhost width="60%" />}>
                    {(loaded) => <ExecutionDetail detail={loaded} />}
                </QueryState>
            )}
        </Drawer>
    )
}

function ExecutionDetail({ detail }: Readonly<{ detail: AdminExecutionDetail }>) {
    const { execution, units, retryableUnits } = detail
    const badge = phaseBadge(execution.phase)
    const retry = useRetryExecution()
    const ended = execution.phase.type === "ended"

    return (
        <Stack gap="md">
            <Group justify="space-between" align="flex-start">
                <Stack gap={2}>
                    <Text fw={600}>
                        {execution.experimentName} in {execution.projectName}
                    </Text>
                    <Text size="xs" c="dimmed">
                        {execution.id} on {execution.dispatcher}, attempt {execution.attempt}
                    </Text>
                </Stack>
                <Badge color={badge.color} variant="light">
                    {badge.label}
                </Badge>
            </Group>
            {execution.phase.type === "ended" && (
                <Text size="sm">
                    {EXIT_REASON_LABELS[execution.phase.reason]}
                    {execution.phase.message !== "" && `: ${execution.phase.message}`}
                </Text>
            )}
            <Text size="sm" c="dimmed">
                {execution.cores} cores, {formatMemory(execution.memoryRequestMb)}, at most{" "}
                {formatDuration(execution.timeLimitSeconds)}
            </Text>
            {retryableUnits > 0 && (
                <Button
                    leftSection={<IconRefresh size={16} />}
                    variant="light"
                    w="fit-content"
                    loading={retry.isPending}
                    onClick={() => retry.mutate(execution.id, { onError: notifyProblem })}
                >
                    Retry {retryableUnits} failed {retryableUnits === 1 ? "unit" : "units"}
                </Button>
            )}
            <Table verticalSpacing={4} fz="sm">
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Scenario</Table.Th>
                        <Table.Th>Seed</Table.Th>
                        <Table.Th>Estimate</Table.Th>
                        <Table.Th>Outcome</Table.Th>
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {units.map((unit) => (
                        <Table.Tr key={`${unit.scenarioIndex}-${unit.seed}`}>
                            <Table.Td>{unit.scenarioIndex}</Table.Td>
                            <Table.Td>{unit.seed}</Table.Td>
                            <Table.Td>
                                {formatDuration(unit.estimatedSeconds)}, {formatMemory(unit.estimatedPeakMemoryMb)}
                            </Table.Td>
                            <Table.Td>{outcomeLabel(unit.outcome)}</Table.Td>
                        </Table.Tr>
                    ))}
                </Table.Tbody>
            </Table>
            <ExecutionLog id={execution.id} available={ended} />
        </Stack>
    )
}
