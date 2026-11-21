"use client"

import {
    MAX_OVERLAID_RUNS,
    type ShownRun,
    describeRuns,
    seriesColor,
    toggleRun,
} from "@/components/experiment/results/resultsView"
import type { ScenarioResults } from "@/lib/experiment/results"
import type { ExperimentSpec } from "@/lib/experiment/spec"
import { Button, Checkbox, Group, Menu, Stack, Text, useComputedColorScheme } from "@mantine/core"
import { IconChevronDown } from "@tabler/icons-react"

export function RunPicker({
    spec,
    scenarios,
    chosen,
    onChange,
}: {
    spec: ExperimentSpec
    scenarios: ScenarioResults[]
    chosen: ShownRun[]
    onChange: (chosen: ShownRun[]) => void
}) {
    const scheme = useComputedColorScheme("light")
    const labels = describeRuns(
        spec,
        scenarios.map((scenario) => scenario.scenarioIndex),
    )

    if (scenarios.length <= 1) return undefined

    return (
        <Menu position="bottom-start" closeOnItemClick={false} withinPortal>
            <Menu.Target>
                <Button variant="default" rightSection={<IconChevronDown size={14} />}>
                    {chosen.length} of {scenarios.length} scenarios
                </Button>
            </Menu.Target>
            <Menu.Dropdown mah={360} style={{ overflowY: "auto" }}>
                <Menu.Label>Overlay up to {MAX_OVERLAID_RUNS} at a time</Menu.Label>
                {scenarios.map((scenario, row) => {
                    const label = labels[row] ?? { short: `#${scenario.scenarioIndex}`, full: "" }
                    const lane = chosen.find((run) => run.scenarioIndex === scenario.scenarioIndex)?.lane
                    return (
                        <Menu.Item
                            key={scenario.scenarioIndex}
                            onClick={() => onChange(toggleRun(chosen, scenario.scenarioIndex))}
                        >
                            <Group gap="sm" wrap="nowrap" align="flex-start">
                                <Checkbox
                                    checked={lane !== undefined}
                                    readOnly
                                    size="xs"
                                    mt={3}
                                    color={lane === undefined ? undefined : seriesColor(lane, scheme)}
                                    aria-label={label.short}
                                />
                                <Stack gap={0}>
                                    <Text size="sm">{label.short}</Text>
                                    <Text size="xs" c="dimmed">
                                        {label.full}
                                    </Text>
                                </Stack>
                            </Group>
                        </Menu.Item>
                    )
                })}
            </Menu.Dropdown>
        </Menu>
    )
}
