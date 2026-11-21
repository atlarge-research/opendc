export const RESULT_TABLES = ["host", "service", "powerSource", "battery"] as const

export type ResultTable = (typeof RESULT_TABLES)[number]

export type ReduceMode = "mean" | "sum" | "max" | "last"

export interface MetricScale {
    unit: string
    scale: number
    decimals: number
}

interface MetricDefinition {
    id: string
    table: ResultTable
    column: string
    label: string
    description: string
    reduce: ReduceMode
    sample: MetricScale
    total: MetricScale
}

const JOULES_PER_KWH = 3_600_000

export const RESULT_METRICS = [
    {
        id: "host.cpu_utilization",
        table: "host",
        column: "cpu_utilization",
        label: "CPU utilization",
        description: "Share of the fleet's CPU capacity that is actually doing work.",
        reduce: "mean",
        // One decimal, or an oversized fleet below 1% reads as a flat zero on every tick.
        sample: { unit: "%", scale: 100, decimals: 1 },
        total: { unit: "%", scale: 100, decimals: 1 },
    },
    {
        id: "host.power_draw",
        table: "host",
        column: "power_draw",
        label: "IT power draw",
        description: "Power drawn by the hosts themselves, summed over the fleet.",
        reduce: "mean",
        sample: { unit: "kW", scale: 0.001, decimals: 1 },
        total: { unit: "kW", scale: 0.001, decimals: 1 },
    },
    {
        id: "powerSource.power_draw",
        table: "powerSource",
        column: "power_draw",
        label: "Facility power draw",
        description: "Power drawn from the power sources, including cooling and conversion losses.",
        reduce: "mean",
        sample: { unit: "kW", scale: 0.001, decimals: 1 },
        total: { unit: "kW", scale: 0.001, decimals: 1 },
    },
    {
        id: "powerSource.energy_usage",
        table: "powerSource",
        column: "energy_usage",
        label: "Energy used",
        description: "Energy drawn from the power sources since the previous sample.",
        reduce: "sum",
        sample: { unit: "kWh", scale: 1 / JOULES_PER_KWH, decimals: 2 },
        total: { unit: "kWh", scale: 1 / JOULES_PER_KWH, decimals: 0 },
    },
    {
        id: "powerSource.carbon_emission",
        table: "powerSource",
        column: "carbon_emission",
        label: "Carbon emitted",
        description: "Operational carbon released since the previous sample.",
        reduce: "sum",
        sample: { unit: "g", scale: 1, decimals: 0 },
        total: { unit: "kg", scale: 0.001, decimals: 1 },
    },
    {
        id: "powerSource.carbon_intensity",
        table: "powerSource",
        column: "carbon_intensity",
        label: "Carbon intensity",
        description: "Carbon released per unit of energy drawn from the grid at that moment.",
        reduce: "mean",
        sample: { unit: "g/kWh", scale: 1, decimals: 0 },
        total: { unit: "g/kWh", scale: 1, decimals: 0 },
    },
    {
        id: "service.tasks_active",
        table: "service",
        column: "tasks_active",
        label: "Tasks running",
        description: "Tasks placed on a host and executing.",
        reduce: "mean",
        sample: { unit: "", scale: 1, decimals: 0 },
        total: { unit: "", scale: 1, decimals: 0 },
    },
    {
        id: "service.tasks_pending",
        table: "service",
        column: "tasks_pending",
        label: "Tasks queued",
        description: "Tasks submitted but not yet placed on a host.",
        reduce: "max",
        sample: { unit: "", scale: 1, decimals: 0 },
        total: { unit: "", scale: 1, decimals: 0 },
    },
    {
        id: "service.tasks_completed",
        table: "service",
        column: "tasks_completed",
        label: "Tasks completed",
        description: "Tasks that ran to completion, counted from the start of the run.",
        reduce: "last",
        sample: { unit: "", scale: 1, decimals: 0 },
        total: { unit: "", scale: 1, decimals: 0 },
    },
    {
        id: "service.tasks_terminated",
        table: "service",
        column: "tasks_terminated",
        label: "Tasks terminated",
        description: "Tasks stopped before completion, counted from the start of the run.",
        reduce: "last",
        sample: { unit: "", scale: 1, decimals: 0 },
        total: { unit: "", scale: 1, decimals: 0 },
    },
    {
        id: "service.hosts_down",
        table: "service",
        column: "hosts_down",
        label: "Hosts down",
        description: "Hosts unavailable because the failure model took them offline.",
        reduce: "max",
        sample: { unit: "", scale: 1, decimals: 0 },
        total: { unit: "", scale: 1, decimals: 0 },
    },
    {
        id: "battery.charge",
        table: "battery",
        column: "charge",
        label: "Battery charge",
        description: "Energy stored in the batteries, summed over the topology.",
        reduce: "mean",
        sample: { unit: "kWh", scale: 1 / JOULES_PER_KWH, decimals: 1 },
        total: { unit: "kWh", scale: 1 / JOULES_PER_KWH, decimals: 1 },
    },
] as const satisfies readonly MetricDefinition[]

export type ResultMetric = (typeof RESULT_METRICS)[number]

export type MetricId = ResultMetric["id"]

export interface ResultPoint {
    t: number
    value: number
}

export interface ResultSeries {
    metric: MetricId
    points: ResultPoint[]
    spread: number
}

export interface ScenarioResults {
    scenarioIndex: number
    seeds: number
    complete: boolean
    series: ResultSeries[]
}

export interface ExperimentResults {
    experimentId: string
    // Resolution the simulation actually exported at.
    exportIntervalMs: number
    // Width of the buckets the server folds a long trace into before reporting it.
    bucketMs: number
    complete: boolean
    scenarios: ScenarioResults[]
}

export function simulatedSpan(results: ExperimentResults): number {
    return results.scenarios.reduce(
        (widest, scenario) =>
            scenario.series.reduce((span, series) => Math.max(span, series.points.at(-1)?.t ?? 0), widest),
        0,
    )
}

export type ReducedValue = { status: "ok"; value: number } | { status: "empty" }

export function isLiveResults(results: ExperimentResults): boolean {
    return results.scenarios.length > 0 && !results.complete
}

export function metricById(id: MetricId): ResultMetric {
    const metric = RESULT_METRICS.find((entry) => entry.id === id)
    if (!metric) throw new Error(`unknown result metric ${id}`)
    return metric
}

export function seriesOf(scenario: ScenarioResults, metric: MetricId): ResultSeries {
    return scenario.series.find((entry) => entry.metric === metric) ?? { metric, points: [], spread: 0 }
}

export function reportedMetrics(results: ExperimentResults): ResultMetric[] {
    const reported = new Set(results.scenarios.flatMap((scenario) => scenario.series.map((entry) => entry.metric)))
    return RESULT_METRICS.filter((metric) => reported.has(metric.id))
}

export function reduceSeries(points: readonly ResultPoint[], mode: ReduceMode): ReducedValue {
    const last = points[points.length - 1]
    if (last === undefined) return { status: "empty" }

    const values = points.map((point) => point.value)
    if (mode === "last") return { status: "ok", value: last.value }
    if (mode === "max") return { status: "ok", value: Math.max(...values) }
    const total = values.reduce((sum, value) => sum + value, 0)
    return { status: "ok", value: mode === "sum" ? total : total / values.length }
}

export function reduceMetric(scenario: ScenarioResults, metric: MetricId): ReducedValue {
    return reduceSeries(seriesOf(scenario, metric).points, metricById(metric).reduce)
}

export interface AlignedRow {
    t: number
    values: Record<number, number>
}

export function alignOnTimestamp(series: ReadonlyArray<readonly ResultPoint[]>): AlignedRow[] {
    const rows = new Map<number, Record<number, number>>()

    series.forEach((points, index) => {
        for (const point of points) {
            const row = rows.get(point.t) ?? {}
            row[index] = point.value
            rows.set(point.t, row)
        }
    })

    return [...rows.entries()].sort(([left], [right]) => left - right).map(([t, values]) => ({ t, values }))
}
