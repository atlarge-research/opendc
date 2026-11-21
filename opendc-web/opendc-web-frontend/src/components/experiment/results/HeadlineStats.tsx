"use client"

import { formatReduced, magnitudeColor } from "@/components/experiment/results/resultsView"
import { type MetricId, RESULT_METRICS, type ScenarioResults, reduceMetric, seriesOf } from "@/lib/experiment/results"
import { Sparkline } from "@mantine/charts"
import { Group, Paper, SimpleGrid, Stack, Text, useComputedColorScheme } from "@mantine/core"

const HEADLINE: MetricId[] = [
    "powerSource.energy_usage",
    "powerSource.carbon_emission",
    "host.cpu_utilization",
    "service.tasks_completed",
]

const SPARK_WIDTH = 64
const SPARK_HEIGHT = 24

export function HeadlineStats({ run }: { run: ScenarioResults }) {
    const scheme = useComputedColorScheme("light")
    const reported = HEADLINE.filter((metric) => seriesOf(run, metric).points.length > 0)

    if (reported.length === 0) return undefined

    return (
        <SimpleGrid cols={{ base: 2, md: reported.length }} spacing="md">
            {reported.map((metric) => (
                <HeadlineTile key={metric} run={run} metric={metric} color={magnitudeColor(scheme)} />
            ))}
        </SimpleGrid>
    )
}

function HeadlineTile({ run, metric, color }: { run: ScenarioResults; metric: MetricId; color: string }) {
    const definition = RESULT_METRICS.find((entry) => entry.id === metric)
    const reduced = reduceMetric(run, metric)
    const scale = definition?.sample.scale ?? 1
    const trace = seriesOf(run, metric).points.map((point) => point.value * scale)

    return (
        <Paper withBorder radius="md" p="sm">
            <Stack gap={4}>
                <Text size="xs" c="dimmed">
                    {definition?.label ?? metric}
                </Text>
                <Group justify="space-between" align="flex-end" wrap="nowrap" gap="xs">
                    <Text size="xl" fw={500} lh={1.1}>
                        {reduced.status === "ok" ? formatReduced(reduced.value, metric) : "--"}
                    </Text>
                    <Sparkline
                        w={SPARK_WIDTH}
                        h={SPARK_HEIGHT}
                        data={trace}
                        color={color}
                        curveType="monotone"
                        fillOpacity={0.25}
                        strokeWidth={2}
                    />
                </Group>
            </Stack>
        </Paper>
    )
}
