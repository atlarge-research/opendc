"use client"

import { comparisonRows, describeRuns, formatScaled, magnitudeColor } from "@/components/experiment/results/resultsView"
import { type MetricId, type ScenarioResults, metricById } from "@/lib/experiment/results"
import type { ExperimentSpec } from "@/lib/experiment/spec"
import { BarChart } from "@mantine/charts"
import { Center, Text, useComputedColorScheme } from "@mantine/core"

const ROW_HEIGHT = 26
const MIN_HEIGHT = 140

export function ScenarioComparison({
    spec,
    scenarios,
    metric,
}: {
    spec: ExperimentSpec
    scenarios: ScenarioResults[]
    metric: MetricId
}) {
    const scheme = useComputedColorScheme("light")
    const definition = metricById(metric)
    const rows = comparisonRows(scenarios, metric)
    const described = new Map(
        describeRuns(
            spec,
            scenarios.map((scenario) => scenario.scenarioIndex),
        ).map((labels, position) => [`#${scenarios[position]?.scenarioIndex}`, labels]),
    )

    if (rows.length === 0) {
        return (
            <Center h={MIN_HEIGHT}>
                <Text size="sm" c="dimmed">
                    Nothing to compare until a scenario reports its first samples.
                </Text>
            </Center>
        )
    }

    return (
        <BarChart
            h={Math.max(MIN_HEIGHT, rows.length * ROW_HEIGHT + 60)}
            data={rows}
            dataKey="run"
            orientation="vertical"
            series={[{ name: "value", label: definition.label, color: magnitudeColor(scheme) }]}
            gridAxis="x"
            tickLine="none"
            maxBarWidth={16}
            barProps={{ radius: [0, 4, 4, 0] }}
            valueFormatter={(value: number) => formatScaled(value, definition.total)}
            yAxisProps={{ width: 48 }}
            tooltipProps={{
                labelFormatter: (label: string) => described.get(label)?.short ?? label,
            }}
            tooltipAnimationDuration={120}
        />
    )
}
