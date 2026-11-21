"use client"

import { PanelGhost } from "@/components/util/Ghost"
import { useScenarioLog } from "@/lib/api/experiments"
import type { Id } from "@/lib/api/types"
import { Code, ScrollArea, Stack, Text } from "@mantine/core"

/** What the launcher wrote while this scenario ran, collected each time an attempt at it ends. */
export function ScenarioLog({
    experimentId,
    scenarioIndex,
    endedAttempts,
}: Readonly<{ experimentId: Id; scenarioIndex: number; endedAttempts: number }>) {
    const log = useScenarioLog(experimentId, scenarioIndex, endedAttempts)

    if (endedAttempts === 0) {
        return (
            <Text size="sm" c="dimmed">
                The log is collected when an attempt ends.
            </Text>
        )
    }
    if (log.isPending) return <PanelGhost height={240} />
    if (log.isError) {
        return (
            <Text size="sm" c="dimmed">
                No log was collected for this scenario.
            </Text>
        )
    }
    return (
        <Stack gap={4}>
            <Text size="sm" fw={500}>
                Log
            </Text>
            <ScrollArea.Autosize mah={420} type="auto">
                <Code block fz="xs">
                    {log.data}
                </Code>
            </ScrollArea.Autosize>
        </Stack>
    )
}
