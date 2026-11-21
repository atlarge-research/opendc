"use client"

import { AXIS_LABELS, axisEntryLabels } from "@/components/experiment/axisLabels"
import { formatCount, formatSimulationBudget } from "@/components/format"
import type { CostEstimate } from "@/lib/api/types"
import {
    AXIS_ORDER,
    type ExperimentSpec,
    experimentAxes,
    experimentRuns,
    positions,
    scenarioCount,
} from "@/lib/experiment/spec"
import { Anchor, Divider, Group, Paper, Stack, Text } from "@mantine/core"
import { useState } from "react"

export function AxisSummary({ spec, estimate }: { spec: ExperimentSpec; estimate: CostEstimate }) {
    const axes = experimentAxes(spec)

    return (
        <Paper withBorder radius="md" p="md">
            <Stack gap="sm">
                {[...AXIS_ORDER].reverse().map((key) => (
                    <AxisRow key={key} label={AXIS_LABELS[key]} entries={axisEntryLabels(axes, key)} />
                ))}

                <Divider my={4} />

                <AxisRow label="Runs per scenario" entries={[formatCount(experimentRuns(spec))]} />
                <AxisRow label="Scenarios" entries={[formatCount(scenarioCount(spec))]} />
                <AxisRow
                    label="Simulation budget"
                    entries={[formatSimulationBudget(estimate.estimatedBudgetSeconds)]}
                />
            </Stack>
        </Paper>
    )
}

// Past this a long axis would push the numbers below off the panel; the rest sit behind a toggle.
const ENTRY_CAP = 4

function AxisRow({ label, entries }: { label: string; entries: string[] }) {
    const [expanded, setExpanded] = useState(false)
    const listed = expanded ? entries : entries.slice(0, ENTRY_CAP)

    return (
        <Group align="flex-start" justify="space-between" wrap="nowrap" gap="lg">
            <Text size="sm" c="dimmed" style={{ flexShrink: 0 }}>
                {label}
            </Text>
            <Stack gap={2} align="flex-end">
                {positions(listed.length).map((position) => (
                    <Text key={position} size="sm" ta="right">
                        {listed[position]}
                    </Text>
                ))}
                {entries.length > ENTRY_CAP && (
                    <Anchor
                        component="button"
                        type="button"
                        size="xs"
                        c="dimmed"
                        onClick={() => setExpanded(!expanded)}
                    >
                        {expanded ? "Show fewer" : `and ${formatCount(entries.length - ENTRY_CAP)} more`}
                    </Anchor>
                )}
            </Stack>
        </Group>
    )
}
