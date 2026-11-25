"use client"

import { ScenarioStateBadge } from "@/components/experiment/ExperimentStateBadge"
import { ScenarioLog } from "@/components/experiment/ScenarioLog"
import { formatBytes } from "@/components/format"
import { LineGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { useScenario } from "@/lib/api/experiments"
import type { Id, ScenarioDetail, ScenarioRun } from "@/lib/api/types"
import { outcomeLabel } from "@/lib/experiment/outcomes"
import { Drawer, Group, Paper, Stack, Text } from "@mantine/core"

export function ScenarioDrawer({
    experimentId,
    scenarioIndex,
    onClose,
}: Readonly<{ experimentId: Id; scenarioIndex: number | undefined; onClose: () => void }>) {
    const detail = useScenario(experimentId, scenarioIndex)
    return (
        <Drawer
            opened={scenarioIndex !== undefined}
            onClose={onClose}
            position="right"
            size="xl"
            title={`Scenario ${scenarioIndex ?? ""}`}
        >
            {scenarioIndex !== undefined && (
                <QueryState query={detail} ghost={<LineGhost width="60%" />}>
                    {(loaded) => <ScenarioView experimentId={experimentId} detail={loaded} />}
                </QueryState>
            )}
        </Drawer>
    )
}

function ScenarioView({ experimentId, detail }: Readonly<{ experimentId: Id; detail: ScenarioDetail }>) {
    const ended = detail.runs.flatMap((run) => run.attempts).filter((attempt) => attempt.outcome.type !== "carried")
    return (
        <Stack gap="md">
            <Group gap="xs">
                <ScenarioStateBadge state={detail.status.state} />
                {detail.status.exitInfo?.message && (
                    <Text size="sm" c="dimmed">
                        {detail.status.exitInfo.message}
                    </Text>
                )}
            </Group>
            {detail.runs.map((run) => (
                <RunCard key={run.seed} run={run} />
            ))}
            <ScenarioLog
                experimentId={experimentId}
                scenarioIndex={detail.status.scenarioIndex}
                endedAttempts={ended.length}
            />
        </Stack>
    )
}

function RunCard({ run }: Readonly<{ run: ScenarioRun }>) {
    return (
        <Paper withBorder radius="md" p="sm">
            <Stack gap={6}>
                <Group justify="space-between">
                    <Text size="sm" fw={500}>
                        Seed {run.seed}
                    </Text>
                    <ScenarioStateBadge state={run.state} />
                </Group>
                {run.attempts.map((attempt) => (
                    <Text key={attempt.executionId} size="xs" c="dimmed">
                        Attempt {attempt.attempt}: {outcomeLabel(attempt.outcome)}
                        {attempt.outcome.type === "failed" &&
                            attempt.outcome.message !== "" &&
                            ` (${attempt.outcome.message})`}
                    </Text>
                ))}
                {run.files.length > 0 && (
                    <Text size="xs" c="dimmed">
                        {run.files.map((file) => `${file.name} (${formatBytes(file.sizeBytes)})`).join(", ")}
                    </Text>
                )}
            </Stack>
        </Paper>
    )
}
