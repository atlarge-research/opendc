"use client"

import { ScenarioStateBadge } from "@/components/experiment/ExperimentStateBadge"
import { ProgressMeter } from "@/components/experiment/ProgressMeter"
import { AXIS_LABELS, axisEntryLabels } from "@/components/experiment/axisLabels"
import { formatPercent } from "@/components/format"
import type { ScenarioStatus } from "@/lib/api/types"
import {
    AXIS_ORDER,
    type AxisKey,
    type ExperimentSpec,
    experimentAxes,
    positions,
    scenarioCoordinates,
    scenarioCount,
} from "@/lib/experiment/spec"
import { isTerminalScenario, progressFraction } from "@/lib/experiment/status"
import { ActionIcon, Anchor, Group, Paper, Stack, Table, Text, Tooltip } from "@mantine/core"
import { IconRefresh } from "@tabler/icons-react"

const MAX_HEIGHT = 520
const INDEX_WIDTH = 56
const STATE_WIDTH = 110
const PROGRESS_WIDTH = 150
const BAR_WIDTH = 90
const BAR_WIDTH_BESIDE_RETRY = 62

// Narrower than this an axis column's entries read as fragments, so the table scrolls instead.
const AXIS_MIN_WIDTH = 150

// Pinned cells need their own background, and Mantine's sticky header sits at zIndex 3 between them.
const PINNED = { bg: "var(--mantine-color-body)", style: { zIndex: 2 } } as const
const PINNED_HEADER = { style: { zIndex: 5 } } as const

// Phones drop the progress column and the state column takes its edge.
const INDEX_COLUMN = { w: INDEX_WIDTH, pos: "sticky", left: 0 } as const
const STATE_COLUMN = { w: STATE_WIDTH, pos: "sticky", right: { base: 0, sm: PROGRESS_WIDTH } } as const
const PROGRESS_COLUMN = { w: PROGRESS_WIDTH, pos: "sticky", right: 0, visibleFrom: "sm" } as const

export function ScenarioTable({
    spec,
    statuses,
    onOpen,
    onRetry,
}: {
    spec: ExperimentSpec
    statuses: ScenarioStatus[]
    onOpen?: (scenarioIndex: number) => void
    onRetry?: (scenarioIndex: number) => void
}) {
    const axes = experimentAxes(spec)
    const total = scenarioCount(spec)
    const varying = [...AXIS_ORDER].reverse().filter((key) => axes[key].length > 1)
    const entryLabels = new Map<AxisKey, string[]>(varying.map((key) => [key, axisEntryLabels(axes, key)]))
    const statusAt = new Map(statuses.map((status) => [status.scenarioIndex, status]))
    const started = statuses.length > 0

    const minWidth = INDEX_WIDTH + varying.length * AXIS_MIN_WIDTH + (started ? STATE_WIDTH + PROGRESS_WIDTH : 0)

    if (total === 0) {
        return (
            <Paper withBorder radius="md" p="md">
                <Text size="sm" c="dimmed">
                    This configuration expands to no scenarios. Every axis needs at least one entry.
                </Text>
            </Paper>
        )
    }

    return (
        <Paper withBorder radius="md" p={0}>
            <Table.ScrollContainer type="native" minWidth={minWidth} maxHeight={MAX_HEIGHT}>
                <Table stickyHeader highlightOnHover verticalSpacing="xs" horizontalSpacing="md">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th {...INDEX_COLUMN} {...PINNED_HEADER}>
                                #
                            </Table.Th>
                            {varying.map((key) => (
                                <Table.Th key={key} miw={AXIS_MIN_WIDTH}>
                                    {AXIS_LABELS[key]}
                                </Table.Th>
                            ))}
                            {started && (
                                <>
                                    <Table.Th {...STATE_COLUMN} {...PINNED_HEADER}>
                                        State
                                    </Table.Th>
                                    <Table.Th {...PROGRESS_COLUMN} {...PINNED_HEADER}>
                                        Progress
                                    </Table.Th>
                                </>
                            )}
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {positions(total).map((scenarioIndex) => {
                            const at = scenarioCoordinates(axes, scenarioIndex)
                            return (
                                <Table.Tr key={`scenario-${scenarioIndex}`}>
                                    <Table.Td {...INDEX_COLUMN} {...PINNED}>
                                        {onOpen === undefined ? (
                                            <Text size="sm" c="dimmed">
                                                {scenarioIndex}
                                            </Text>
                                        ) : (
                                            <Anchor
                                                component="button"
                                                size="sm"
                                                aria-label={`Open scenario ${scenarioIndex}`}
                                                onClick={() => onOpen(scenarioIndex)}
                                            >
                                                {scenarioIndex}
                                            </Anchor>
                                        )}
                                    </Table.Td>
                                    {varying.map((key) => (
                                        <Table.Td key={key}>
                                            <Text size="sm">{entryLabels.get(key)?.[at[key]] ?? ""}</Text>
                                        </Table.Td>
                                    ))}
                                    {started && (
                                        <ScenarioOutcome status={statusAt.get(scenarioIndex)} onRetry={onRetry} />
                                    )}
                                </Table.Tr>
                            )
                        })}
                    </Table.Tbody>
                </Table>
            </Table.ScrollContainer>
        </Paper>
    )
}

function ScenarioOutcome({
    status,
    onRetry,
}: {
    status: ScenarioStatus | undefined
    onRetry?: (scenarioIndex: number) => void
}) {
    if (status === undefined) {
        return (
            <>
                <Table.Td {...STATE_COLUMN} {...PINNED}>
                    <Text size="sm" c="dimmed">
                        Queued
                    </Text>
                </Table.Td>
                <Table.Td {...PROGRESS_COLUMN} {...PINNED} />
            </>
        )
    }

    const fraction = progressFraction(status)
    const exit = status.exitInfo

    return (
        <>
            <Table.Td {...STATE_COLUMN} {...PINNED}>
                <Stack gap={2} align="flex-start">
                    {exit === undefined ? (
                        <ScenarioStateBadge state={status.state} />
                    ) : (
                        <Tooltip label={exit.message ?? `exit ${exit.exitCode}`} withArrow>
                            <span>
                                <ScenarioStateBadge state={status.state} />
                            </span>
                        </Tooltip>
                    )}
                    {/* Stands in for the progress column, which phones hide. */}
                    <Text size="xs" c="dimmed" hiddenFrom="sm">
                        {formatPercent(fraction)}
                    </Text>
                </Stack>
            </Table.Td>
            <Table.Td {...PROGRESS_COLUMN} {...PINNED}>
                <Group gap="xs" wrap="nowrap">
                    <ProgressMeter
                        fraction={fraction}
                        failed={status.state === "failed"}
                        width={onRetry === undefined ? BAR_WIDTH : BAR_WIDTH_BESIDE_RETRY}
                        size="sm"
                        label={`Scenario ${status.scenarioIndex} progress`}
                    />
                    {onRetry !== undefined && isTerminalScenario(status.state) && (
                        <Tooltip label="Run this scenario again" withArrow>
                            <ActionIcon
                                variant="subtle"
                                color="gray"
                                size="sm"
                                aria-label={`Run scenario ${status.scenarioIndex} again`}
                                onClick={() => onRetry(status.scenarioIndex)}
                            >
                                <IconRefresh size={14} />
                            </ActionIcon>
                        </Tooltip>
                    )}
                </Group>
            </Table.Td>
        </>
    )
}
