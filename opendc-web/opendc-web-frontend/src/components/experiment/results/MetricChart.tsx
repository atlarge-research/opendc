"use client"

import { type LegendEntry, SeriesLegend } from "@/components/experiment/results/SeriesLegend"
import {
    type RunLabels,
    formatScaled,
    formatSimulatedInstant,
    formatTick,
    tickAxisWidth,
    timeAxis,
    timeRows,
} from "@/components/experiment/results/resultsView"
import { type MetricId, type ScenarioResults, metricById } from "@/lib/experiment/results"
import { LineChart } from "@mantine/charts"
import { Center, Stack, Text } from "@mantine/core"
import { useState } from "react"

const CHART_HEIGHT = 300

/** A line the reader has looked away from. It keeps its place on the axis without being drawn. */
const HIDDEN = "transparent"

/** One scenario's line, in the colour it keeps for as long as it is on the chart. */
export interface PlottedRun {
    scenario: ScenarioResults
    label: RunLabels
    color: string
}

export function legendOf(lines: PlottedRun[]): LegendEntry[] {
    return lines.map((line) => ({ name: line.label.short, color: line.color }))
}

export function MetricChart({ lines, metric, spanMs }: { lines: PlottedRun[]; metric: MetricId; spanMs: number }) {
    const [isolated, setIsolated] = useState<string | undefined>(undefined)
    const definition = metricById(metric)
    const keys = lines.map((line) => line.label.short)
    const rows = timeRows(
        lines.map((line) => line.scenario),
        metric,
        keys,
    )

    if (rows.length === 0) {
        return (
            <Center h={CHART_HEIGHT}>
                <Text size="sm" c="dimmed">
                    No samples yet. The first ones arrive once a scenario starts simulating.
                </Text>
            </Center>
        )
    }

    // A run that reported one instant has no span to divide up; the axis still needs a width.
    const span = Math.max(spanMs, 1)
    const axis = timeAxis(span)
    const entries = legendOf(lines)
    // Isolated by colour, not by removing lines, so the scale holds still while the pointer moves.
    const series = entries.map((entry) => ({
        name: entry.name,
        color: isolated === undefined || isolated === entry.name ? entry.color : HIDDEN,
    }))

    return (
        <Stack gap="xs">
            <LineChart
                h={CHART_HEIGHT}
                data={rows}
                dataKey="t"
                series={series}
                curveType="monotone"
                strokeWidth={2}
                withDots={false}
                connectNulls={false}
                gridAxis="xy"
                tickLine="none"
                // Mantine's legend only dims the other runs, which does not help a sweep of a dozen.
                withLegend={false}
                valueFormatter={(value: number) => formatScaled(value, definition.sample)}
                yAxisLabel={definition.sample.unit === "" ? undefined : definition.sample.unit}
                yAxisProps={{
                    tickFormatter: (value: number) => formatTick(value, definition.sample),
                    width: tickAxisWidth(rows, keys, definition.sample),
                    allowDecimals: definition.sample.decimals > 0,
                }}
                // Numeric, not categorical, so points sit at their real time and ticks on round spans.
                xAxisProps={{
                    type: "number",
                    domain: [0, span],
                    ticks: axis.ticks,
                    tickFormatter: axis.format,
                    minTickGap: 48,
                }}
                tooltipProps={{ labelFormatter: (value: number) => formatSimulatedInstant(value, spanMs) }}
                tooltipAnimationDuration={120}
            />
            {entries.length > 1 && <SeriesLegend entries={entries} isolated={isolated} onIsolate={setIsolated} />}
        </Stack>
    )
}
