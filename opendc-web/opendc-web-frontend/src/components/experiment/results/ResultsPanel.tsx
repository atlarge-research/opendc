"use client"

import { ChartCard } from "@/components/experiment/results/ChartCard"
import { HeadlineStats } from "@/components/experiment/results/HeadlineStats"
import { MetricChart, type PlottedRun, legendOf } from "@/components/experiment/results/MetricChart"
import { RunPicker } from "@/components/experiment/results/RunPicker"
import { ScenarioComparison } from "@/components/experiment/results/ScenarioComparison"
import {
    type ColorScheme,
    type ShownRun,
    defaultRuns,
    describeRuns,
    formatInterval,
    formatSimulatedDuration,
    metricOptions,
    resultsCsv,
    seriesColor,
} from "@/components/experiment/results/resultsView"
import { PanelGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { downloadText, fileSlug } from "@/components/util/download"
import { notifyProblem } from "@/components/util/feedback"
import { startDownload } from "@/lib/api/downloads"
import { resultsArchiveLink, useExperimentResults } from "@/lib/api/experiments"
import type { Experiment } from "@/lib/api/types"
import {
    type ExperimentResults,
    type MetricId,
    RESULT_METRICS,
    type ResultMetric,
    reportedMetrics,
    simulatedSpan,
} from "@/lib/experiment/results"
import type { ExperimentSpec } from "@/lib/experiment/spec"
import { Alert, Button, Group, Select, Stack, Text, useComputedColorScheme } from "@mantine/core"
import { IconDownload, IconFileZip, IconFlask } from "@tabler/icons-react"
import { useState } from "react"

const DEFAULT_METRIC: MetricId = "host.cpu_utilization"

export function ResultsPanel({ experiment }: { experiment: Experiment }) {
    const results = useExperimentResults(experiment.id)

    if (experiment.state === "draft") {
        return (
            <Alert color="gray" icon={<IconFlask size={18} />} title="Nothing has run yet">
                Results appear here scenario by scenario once the experiment runs.
            </Alert>
        )
    }

    return (
        <QueryState query={results} ghost={<PanelGhost height={260} />}>
            {(loaded) => <LoadedResults experiment={experiment} results={loaded} />}
        </QueryState>
    )
}

function LoadedResults({ experiment, results }: { experiment: Experiment; results: ExperimentResults }) {
    const reported = reportedMetrics(results)
    const scheme = useComputedColorScheme("light")
    const [preferred, setPreferred] = useState<MetricId>(DEFAULT_METRIC)
    const [picked, setPicked] = useState<ShownRun[]>([])

    // Parquet lands per scenario, so anything past queued has files before it has samples to chart.
    const downloads = experiment.state !== "queued" && <ResultDownloads experiment={experiment} results={results} />

    if (reported.length === 0) {
        return (
            <Stack gap="md">
                {downloads && <Group justify="flex-end">{downloads}</Group>}
                <Alert color="gray" icon={<IconFlask size={18} />} title="Waiting for the first samples">
                    Every scenario is still queued. Measurements stream in at the export interval once one starts.
                </Alert>
            </Stack>
        )
    }

    const metric = pickMetric(reported, preferred)
    const shown = pickRuns(results, picked)
    const lines = plottedRuns(results, experiment.spec, shown, scheme)
    const primary = lines[0]
    const spanMs = simulatedSpan(results)
    const fileName = fileSlug(`${experiment.name} ${metric.label}`)

    return (
        <Stack gap="md">
            <Group justify="space-between" wrap="wrap" gap="sm">
                <Group gap="sm" wrap="wrap">
                    <Select
                        data={metricOptions(results)}
                        value={metric.id}
                        onChange={(value) => setPreferred(metricIdOf(value, metric.id))}
                        allowDeselect={false}
                        w={220}
                        aria-label="Metric"
                    />
                    <RunPicker
                        spec={experiment.spec}
                        scenarios={results.scenarios}
                        chosen={shown}
                        onChange={setPicked}
                    />
                </Group>
                {downloads}
            </Group>

            {primary && (
                <Stack gap={6}>
                    <Text size="xs" c="dimmed">
                        {primary.label.short} totals
                    </Text>
                    <HeadlineStats run={primary.scenario} />
                </Stack>
            )}

            <ChartCard
                title={`${metric.label} over simulated time`}
                caption={`${formatSimulatedDuration(spanMs)} simulated, one point per ${formatInterval(results.bucketMs)}`}
                hint={metric.description}
                fileName={`${fileName}.png`}
                // A saved image has no run picker beside it, so it names the scenario even when it is alone.
                legend={results.scenarios.length > 1 ? legendOf(lines) : []}
            >
                <MetricChart lines={lines} metric={metric.id} spanMs={spanMs} />
            </ChartCard>

            {results.scenarios.length > 1 && (
                <ChartCard
                    title={`${metric.label} per scenario`}
                    caption={`All ${results.scenarios.length} scenarios, reduced over each whole run`}
                    hint={`Every scenario reduced to a single number by taking the ${metric.reduce} of its samples.`}
                    fileName={`${fileName}-per-scenario.png`}
                    legend={[]}
                >
                    <ScenarioComparison spec={experiment.spec} scenarios={results.scenarios} metric={metric.id} />
                </ChartCard>
            )}
        </Stack>
    )
}

function ResultDownloads({ experiment, results }: { experiment: Experiment; results: ExperimentResults }) {
    return (
        <Group gap="sm" wrap="wrap">
            {results.scenarios.length > 0 && (
                <Button
                    variant="default"
                    leftSection={<IconDownload size={16} />}
                    onClick={() => downloadCsv(experiment.name, results)}
                >
                    Export CSV
                </Button>
            )}
            <Button
                variant="default"
                leftSection={<IconFileZip size={16} />}
                onClick={() => startDownload(resultsArchiveLink(experiment.id)).catch(notifyProblem)}
            >
                Download all results
            </Button>
        </Group>
    )
}

function pickMetric(reported: ResultMetric[], preferred: MetricId): ResultMetric {
    const chosen = reported.find((metric) => metric.id === preferred) ?? reported[0]
    if (!chosen) throw new Error("no metric to show")
    return chosen
}

function metricIdOf(raw: string | null, fallback: MetricId): MetricId {
    return RESULT_METRICS.find((metric) => metric.id === raw)?.id ?? fallback
}

function pickRuns(results: ExperimentResults, picked: ShownRun[]): ShownRun[] {
    const available = results.scenarios.map((scenario) => scenario.scenarioIndex)
    const kept = picked.filter((run) => available.includes(run.scenarioIndex))
    return kept.length > 0 ? kept : defaultRuns(available)
}

// In scenario order, so the legend and the totals read the same whichever run was picked first.
function plottedRuns(
    results: ExperimentResults,
    spec: ExperimentSpec,
    shown: ShownRun[],
    scheme: ColorScheme,
): PlottedRun[] {
    const ordered = [...shown].sort((left, right) => left.scenarioIndex - right.scenarioIndex)
    const labels = describeRuns(
        spec,
        ordered.map((run) => run.scenarioIndex),
    )
    return ordered.flatMap((run, position) => {
        const scenario = results.scenarios.find((entry) => entry.scenarioIndex === run.scenarioIndex)
        const label = labels[position]
        if (scenario === undefined || label === undefined) return []
        return [{ scenario, label, color: seriesColor(run.lane, scheme) }]
    })
}

function downloadCsv(name: string, results: ExperimentResults): void {
    downloadText(`${fileSlug(name)}-results.csv`, resultsCsv(results), "text/csv")
}
