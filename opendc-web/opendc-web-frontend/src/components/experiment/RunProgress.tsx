"use client"

import { formatCount, formatDuration, formatPercent, formatUpdatedAt } from "@/components/format"
import type { Experiment, ExperimentStatus } from "@/lib/api/types"
import {
    SCENARIO_STATES,
    type ScenarioExecutionState,
    isTerminalExperiment,
    progressFraction,
} from "@/lib/experiment/status"
import { Group, Paper, Progress, Stack, Text } from "@mantine/core"

const DOT_COLORS: Record<ScenarioExecutionState, string> = {
    queued: "gray.5",
    running: "opendc.6",
    succeeded: "green.6",
    failed: "red.6",
    cancelled: "gray.6",
}

export function RunProgress({ experiment, status }: { experiment: Experiment; status: ExperimentStatus }) {
    const fraction = progressFraction(status)
    const settled = isTerminalExperiment(experiment.state)

    return (
        <Paper withBorder radius="md" p="md">
            <Stack gap="sm">
                <Group justify="space-between" align="flex-end" gap="xs">
                    <Text size="sm" c="dimmed">
                        {formatCount(status.completedTasks)} of {formatCount(status.totalTasks)} tasks simulated
                    </Text>
                    <Text size="sm">{formatPercent(fraction)}</Text>
                </Group>

                <Progress
                    value={fraction * 100}
                    color={experiment.state === "failed" ? "red" : "opendc"}
                    animated={!settled}
                    aria-label="Experiment progress"
                />

                <Group gap="lg" wrap="wrap">
                    {SCENARIO_STATES.map((state) => (
                        <StateCount key={state} state={state} statuses={status.scenarios} />
                    ))}
                    <Timing experiment={experiment} settled={settled} />
                </Group>
            </Stack>
        </Paper>
    )
}

function StateCount({
    state,
    statuses,
}: {
    state: ScenarioExecutionState
    statuses: ExperimentStatus["scenarios"]
}) {
    const count = statuses.filter((entry) => entry.state === state).length
    if (count === 0) return undefined

    return (
        <Group gap={6} wrap="nowrap">
            <Text size="xs" c={DOT_COLORS[state]}>
                &#9679;
            </Text>
            <Text size="xs" c="dimmed">
                {formatCount(count)} {state}
            </Text>
        </Group>
    )
}

function Timing({ experiment, settled }: { experiment: Experiment; settled: boolean }) {
    const submittedAt = experiment.submittedAt
    if (submittedAt === undefined) return undefined

    const elapsed = (Date.now() - Date.parse(submittedAt)) / 1000

    return (
        <Text size="xs" c="dimmed" ml="auto">
            {settled ? `Started ${formatUpdatedAt(submittedAt)}` : `Running for ${formatDuration(elapsed)}`}
        </Text>
    )
}
