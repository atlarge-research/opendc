import { AXIS_LABELS, axisEntryLabels } from "@/components/experiment/axisLabels"
import {
    type ExperimentResults,
    type MetricId,
    type MetricScale,
    type ScenarioResults,
    alignOnTimestamp,
    metricById,
    reduceMetric,
    reportedMetrics,
    seriesOf,
} from "@/lib/experiment/results"
import {
    AXIS_ORDER,
    type AxisKey,
    type ExperimentSpec,
    experimentAxes,
    scenarioCoordinates,
} from "@/lib/experiment/spec"

// Every pair holds apart, colour-blind readers included, on the #ffffff and #242424 surfaces.
const SERIES_COLORS: Record<ColorScheme, string[]> = {
    light: ["#2a78d6", "#eb6834", "#1baf7a"],
    dark: ["#3987e5", "#d95926", "#199e70"],
}

export type ColorScheme = "light" | "dark"

export const MAX_OVERLAID_RUNS = SERIES_COLORS.light.length

export function seriesColor(position: number, scheme: ColorScheme): string {
    const palette = SERIES_COLORS[scheme]
    return palette[position % palette.length] ?? palette[0] ?? "gray"
}

export function magnitudeColor(scheme: ColorScheme): string {
    return seriesColor(0, scheme)
}

/** A scenario on the time chart, and the lane, so the colour, it keeps for as long as it stays there. */
export interface ShownRun {
    scenarioIndex: number
    lane: number
}

// The first scenarios each own a lane, so an experiment of up to three always draws a scenario in the
// same colour. Any other scenario takes the first lane left free.
function laneFor(shown: ShownRun[], scenarioIndex: number): number {
    const taken = new Set(shown.map((run) => run.lane))
    if (scenarioIndex < MAX_OVERLAID_RUNS && !taken.has(scenarioIndex)) return scenarioIndex
    let lane = 0
    while (taken.has(lane)) lane += 1
    return lane
}

/** What the time chart shows until the reader picks: the first scenarios to report. */
export function defaultRuns(available: number[]): ShownRun[] {
    const shown: ShownRun[] = []
    for (const scenarioIndex of available.slice(0, MAX_OVERLAID_RUNS)) {
        shown.push({ scenarioIndex, lane: laneFor(shown, scenarioIndex) })
    }
    return shown
}

// Taking a run off leaves every other run its colour; adding one to a full chart takes the oldest off.
// The last run stays, so the chart is never empty.
export function toggleRun(shown: ShownRun[], scenarioIndex: number): ShownRun[] {
    if (shown.some((run) => run.scenarioIndex === scenarioIndex)) {
        const kept = shown.filter((run) => run.scenarioIndex !== scenarioIndex)
        return kept.length > 0 ? kept : shown
    }
    const room = shown.length < MAX_OVERLAID_RUNS ? shown : shown.slice(1)
    return [...room, { scenarioIndex, lane: laneFor(room, scenarioIndex) }]
}

export interface RunLabels {
    short: string
    full: string
}

const SHORT_LABEL_AXES = 2

const LABEL_SEPARATOR = " \xb7 "

// Short names use only the axes that differ within the shown set, or overlaid lines share a label.
export function describeRuns(spec: ExperimentSpec, scenarioIndices: number[]): RunLabels[] {
    const axes = experimentAxes(spec)
    const varying = [...AXIS_ORDER].reverse().filter((key) => axes[key].length > 1)
    const coordinates = scenarioIndices.map((index) => scenarioCoordinates(axes, index))
    const distinguishing = varying.filter((key) => new Set(coordinates.map((at) => at[key])).size > 1)

    return scenarioIndices.map((scenarioIndex, position) => {
        const at = coordinates[position]
        const entryAt = (key: AxisKey) => (at === undefined ? "" : (axisEntryLabels(axes, key)[at[key]] ?? ""))
        const naming = distinguishing.slice(0, SHORT_LABEL_AXES).map(entryAt).join(LABEL_SEPARATOR)

        return {
            short: naming === "" ? `#${scenarioIndex}` : `#${scenarioIndex} ${naming}`,
            full:
                varying.length === 0
                    ? "The only scenario in this experiment"
                    : varying.map((key) => `${AXIS_LABELS[key]}: ${entryAt(key)}`).join(LABEL_SEPARATOR),
        }
    })
}

export function metricOptions(results: ExperimentResults): Array<{ value: string; label: string }> {
    return reportedMetrics(results).map((metric) => ({ value: metric.id, label: metric.label }))
}

// The server folds every scenario onto one grid, so their points already share instants.
export function timeRows(
    scenarios: ScenarioResults[],
    metric: MetricId,
    keys: string[],
): Array<Record<string, number>> {
    const scale = metricById(metric).sample.scale
    return alignOnTimestamp(scenarios.map((scenario) => seriesOf(scenario, metric).points)).map((row) => {
        const shaped: Record<string, number> = { t: row.t }
        for (const [position, key] of keys.entries()) {
            const value = row.values[position]
            if (value !== undefined) shaped[key] = value * scale
        }
        return shaped
    })
}

export interface ComparisonRow {
    run: string
    value: number
    spread: number
}

export function comparisonRows(scenarios: ScenarioResults[], metric: MetricId): ComparisonRow[] {
    const scale = metricById(metric).total.scale
    return scenarios.flatMap((scenario) => {
        const reduced = reduceMetric(scenario, metric)
        if (reduced.status === "empty") return []
        return [
            {
                run: `#${scenario.scenarioIndex}`,
                value: reduced.value * scale,
                spread: seriesOf(scenario, metric).spread * scale,
            },
        ]
    })
}

function formatNumber(value: number, decimals: number): string {
    return new Intl.NumberFormat("en-GB", {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals,
    }).format(value)
}

export function formatScaled(value: number, scale: MetricScale): string {
    const shown = formatNumber(value, scale.decimals)
    return scale.unit === "" ? shown : `${shown} ${scale.unit}`
}

/** A tick on a value axis: a round number shows only the decimals it has, and the axis label carries the unit. */
export function formatTick(value: number, scale: MetricScale): string {
    return new Intl.NumberFormat("en-GB", { maximumFractionDigits: scale.decimals }).format(value)
}

// A glyph at the axis's 12px, and the gap a tick keeps from the axis line.
const TICK_CHAR_PX = 7
const TICK_GAP_PX = 16

// Recharts gives a value axis a fixed width. It never rounds the top tick up past twice the largest
// value, so that number at full precision is as wide as any label on the axis can get.
export function tickAxisWidth(rows: Array<Record<string, number>>, keys: string[], scale: MetricScale): number {
    const largest = Math.max(0, ...rows.flatMap((row) => keys.map((key) => Math.abs(row[key] ?? 0))))
    return formatNumber(2 * largest, scale.decimals).length * TICK_CHAR_PX + TICK_GAP_PX
}

export function formatReduced(value: number, metric: MetricId): string {
    const scale = metricById(metric).total
    return formatScaled(value * scale.scale, scale)
}

const MINUTE_MS = 60_000
const HOUR_MS = 3_600_000
const DAY_MS = 86_400_000
const WEEK_MS = 7 * DAY_MS

// Ticks land on exact multiples of one of these, so every label is a whole number of its unit.
const TICK_STEPS = [
    MINUTE_MS,
    5 * MINUTE_MS,
    15 * MINUTE_MS,
    30 * MINUTE_MS,
    HOUR_MS,
    2 * HOUR_MS,
    3 * HOUR_MS,
    6 * HOUR_MS,
    12 * HOUR_MS,
    DAY_MS,
    2 * DAY_MS,
    3 * DAY_MS,
    WEEK_MS,
    2 * WEEK_MS,
    4 * WEEK_MS,
    13 * WEEK_MS,
    26 * WEEK_MS,
    52 * WEEK_MS,
]

const TARGET_TICKS = 8

export interface TimeAxis {
    ticks: number[]
    format: (milliseconds: number) => string
}

// Ticks and their unit are chosen together, so no two labels collide and none is skipped.
export function timeAxis(spanMs: number): TimeAxis {
    const widest = TICK_STEPS[TICK_STEPS.length - 1] ?? DAY_MS
    const step = TICK_STEPS.find((candidate) => spanMs / candidate <= TARGET_TICKS) ?? widest
    const ticks: number[] = []
    for (let at = 0; at <= spanMs; at += step) {
        ticks.push(at)
    }
    return { ticks, format: labelEvery(step) }
}

function labelEvery(step: number): (milliseconds: number) => string {
    if (step >= WEEK_MS) return (milliseconds) => `${milliseconds / WEEK_MS}w`
    if (step >= DAY_MS) return (milliseconds) => `${milliseconds / DAY_MS}d`
    if (step >= HOUR_MS) return (milliseconds) => `${milliseconds / HOUR_MS}h`
    return (milliseconds) => `${milliseconds / MINUTE_MS}m`
}

function clockOf(milliseconds: number): string {
    const hours = Math.floor(milliseconds / HOUR_MS)
    const minutes = Math.floor((milliseconds % HOUR_MS) / MINUTE_MS)
    return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}`
}

// A bare clock is ambiguous on a long trace, so the day is named once the run spans more than one.
export function formatSimulatedInstant(milliseconds: number, spanMs: number): string {
    const clock = clockOf(milliseconds % DAY_MS)
    return spanMs < DAY_MS ? clock : `Day ${Math.floor(milliseconds / DAY_MS) + 1}, ${clock}`
}

export function formatSimulatedDuration(milliseconds: number): string {
    if (milliseconds >= DAY_MS) return plural(Math.round(milliseconds / DAY_MS), "day")
    if (milliseconds >= HOUR_MS) return plural(Math.round(milliseconds / HOUR_MS), "hour")
    return plural(Math.max(1, Math.round(milliseconds / MINUTE_MS)), "minute")
}

function plural(count: number, unit: string): string {
    return `${count} ${unit}${count === 1 ? "" : "s"}`
}

// Spelled out in full, since a point covering 80 minutes is not one per hour. "Every 1 day" reads badly
// where "every day" does not.
export function formatInterval(milliseconds: number): string {
    const minutes = Math.max(1, Math.round(milliseconds / MINUTE_MS))
    const parts: Array<[number, string]> = [
        [Math.floor(minutes / (DAY_MS / MINUTE_MS)), "day"],
        [Math.floor((minutes % (DAY_MS / MINUTE_MS)) / (HOUR_MS / MINUTE_MS)), "hour"],
        [minutes % (HOUR_MS / MINUTE_MS), "minute"],
    ]
    const spelled = parts
        .filter(([count]) => count > 0)
        .map(([count, unit]) => plural(count, unit))
        .join(" ")
    return /^1 [a-z]+$/.test(spelled) ? spelled.slice(2) : spelled
}

export function resultsCsv(results: ExperimentResults): string {
    const metrics = reportedMetrics(results)
    const header = ["scenario", "simulated_ms", ...metrics.map((metric) => metric.column)]
    const lines = [header.join(",")]

    for (const scenario of results.scenarios) {
        const stamps = alignOnTimestamp(metrics.map((metric) => seriesOf(scenario, metric.id).points))
        for (const row of stamps) {
            const cells = metrics.map((_, position) => row.values[position]?.toString() ?? "")
            lines.push([scenario.scenarioIndex, row.t, ...cells].join(","))
        }
    }

    return lines.join("\n")
}
