import {
    type ShownRun,
    defaultRuns,
    formatInterval,
    formatScaled,
    formatTick,
    tickAxisWidth,
    timeAxis,
    toggleRun,
} from "@/components/experiment/results/resultsView"
import {
    RESULT_METRICS,
    type ResultPoint,
    type ScenarioResults,
    alignOnTimestamp,
    isLiveResults,
    metricById,
    reduceMetric,
    reduceSeries,
    seriesOf,
} from "@/lib/experiment/results"
import { describe, expect, it } from "vitest"

// The interval the simulator exports at by default. The server reports the real one per
// experiment; these tests only need a fixed spacing to lay samples out on.
const EXPORT_INTERVAL_MS = 300_000

function points(...values: number[]): ResultPoint[] {
    return values.map((value, index) => ({ t: index * EXPORT_INTERVAL_MS, value }))
}

function scenario(overrides: Partial<ScenarioResults> = {}): ScenarioResults {
    return { scenarioIndex: 0, seeds: 1, complete: true, series: [], ...overrides }
}

const HALF_AN_HOUR = 30 * 60_000
const A_SHIFT = 6 * 3_600_000
const A_FEW_DAYS = 3 * 86_400_000
const A_MONTH = 30 * 86_400_000
const A_LONG_TRACE = 179 * 86_400_000
const YEARS = 3 * 365 * 86_400_000

// Marks left wherever a sample happens to fall and then labelled in whole weeks put one week on the
// axis twice and leave the next off it, which reads as a chart that has lost track of its own time.
describe("the simulated time axis", () => {
    const spans = [HALF_AN_HOUR, A_SHIFT, A_FEW_DAYS, A_MONTH, A_LONG_TRACE, YEARS]

    it("never puts the same label on the axis twice, however long the run", () => {
        for (const span of spans) {
            const { ticks, format } = timeAxis(span)
            const labels = ticks.map(format)

            expect(new Set(labels).size).toBe(labels.length)
        }
    })

    it("leaves the same distance between every pair of marks, so none is skipped", () => {
        const { ticks } = timeAxis(A_LONG_TRACE)
        const gaps = new Set(ticks.slice(1).map((at, index) => at - (ticks[index] ?? 0)))

        expect(gaps.size).toBe(1)
    })

    it("keeps enough marks to read the run by without crowding the axis", () => {
        for (const span of spans) {
            const { ticks } = timeAxis(span)

            expect(ticks.length).toBeGreaterThanOrEqual(2)
            expect(ticks.length).toBeLessThanOrEqual(9)
        }
    })

    it("counts from the start of the run in whole units of the step it chose", () => {
        expect(timeAxis(120 * 86_400_000).ticks.map(timeAxis(120 * 86_400_000).format)).toEqual([
            "0w",
            "4w",
            "8w",
            "12w",
            "16w",
        ])
    })
})

// A fleet sized well past its workload runs at a fraction of a percent, which is a real reading and
// not a zero. Rounded away, the chart reads as flat and every axis tick carries the same label.
describe("showing a share of a large fleet", () => {
    const utilization = metricById("host.cpu_utilization")

    it("keeps a sub-percent share legible instead of rounding it to nothing", () => {
        expect(formatScaled(0.694, utilization.sample)).toBe("0.7 %")
        expect(formatScaled(1.204, utilization.sample)).toBe("1.2 %")
    })

    it("gives neighbouring axis ticks different labels at the scale a large fleet runs at", () => {
        const ticks = [0.5, 1, 1.5, 2].map((tick) => formatTick(tick, utilization.sample))

        expect(new Set(ticks).size).toBe(ticks.length)
    })

    it("still reads plainly for a fleet that is busy", () => {
        expect(formatScaled(42.9, utilization.sample)).toBe("42.9 %")
    })
})

// The unit sits in the axis label, so ".00 kWh" on every tick only widened the axis until its labels
// ran off the left of the chart.
describe("value-axis ticks", () => {
    const energy = metricById("powerSource.energy_usage")

    it("shows a round tick as the number it is, without trailing decimals or a unit", () => {
        expect(formatTick(300_000, energy.sample)).toBe("300,000")
        expect(formatTick(2.5, energy.sample)).toBe("2.5")
    })

    it("widens the axis for longer labels rather than letting them run off the chart", () => {
        const narrow = tickAxisWidth([{ t: 0, run: 9 }], ["run"], energy.sample)
        const wide = tickAxisWidth([{ t: 0, run: 287_543 }], ["run"], energy.sample)

        expect(wide).toBeGreaterThan(narrow)
    })

    it("sizes the axis by the values reported, not by the instants a run has no value at", () => {
        expect(tickAxisWidth([{ t: 0 }, { t: 1, run: 5 }], ["run"], energy.sample)).toBe(
            tickAxisWidth([{ t: 1, run: 5 }], ["run"], energy.sample),
        )
    })
})

// The server widens a point by doubling, so a point can cover 80 minutes; calling that "one per hour"
// misstates what each point sums or averages over.
describe("the interval one point covers", () => {
    it("names a whole unit on its own", () => {
        expect(formatInterval(3_600_000)).toBe("hour")
        expect(formatInterval(86_400_000)).toBe("day")
        expect(formatInterval(5 * 60_000)).toBe("5 minutes")
    })

    it("spells out every unit of an interval that is not a whole one", () => {
        expect(formatInterval(80 * 60_000)).toBe("1 hour 20 minutes")
        expect(formatInterval(320 * 60_000)).toBe("5 hours 20 minutes")
        expect(formatInterval(2560 * 60_000)).toBe("1 day 18 hours 40 minutes")
    })
})

// A colour names a scenario. Repainting the runs that stay when one is taken off makes the reader
// relearn which line is which.
describe("the colours of overlaid runs", () => {
    const lanes = (shown: ShownRun[]) => Object.fromEntries(shown.map((run) => [run.scenarioIndex, run.lane]))

    it("keeps every run's colour when another is taken off", () => {
        expect(lanes(toggleRun(defaultRuns([0, 1, 2]), 1))).toEqual({ 0: 0, 2: 2 })
    })

    it("gives a run its own colour back when it returns", () => {
        const returned = toggleRun(toggleRun(defaultRuns([0, 1, 2]), 1), 1)

        expect(lanes(returned)).toEqual({ 0: 0, 1: 1, 2: 2 })
    })

    it("takes the oldest run off a full chart and gives the newcomer its colour, leaving the rest alone", () => {
        expect(lanes(toggleRun(defaultRuns([0, 1, 2, 3]), 3))).toEqual({ 1: 1, 2: 2, 3: 0 })
    })

    it("never takes the last run off the chart", () => {
        const alone = toggleRun(toggleRun(defaultRuns([0, 1]), 1), 0)

        expect(lanes(alone)).toEqual({ 0: 0 })
    })

    it("draws the first scenarios in their own colours even when one of them reports late", () => {
        expect(lanes(defaultRuns([0, 2]))).toEqual({ 0: 0, 2: 2 })
        expect(lanes(defaultRuns([0, 1, 2]))).toEqual({ 0: 0, 1: 1, 2: 2 })
    })

    it("gives scenarios past the first three the colours left free", () => {
        expect(lanes(defaultRuns([1, 3, 4]))).toEqual({ 1: 1, 3: 0, 4: 2 })
    })
})

describe("the metric catalog", () => {
    it("identifies every metric by the export table and column it is read from", () => {
        for (const metric of RESULT_METRICS) {
            expect(metric.id).toBe(`${metric.table}.${metric.column}`)
        }
    })

    it("has no duplicate identities, so a chart can never read the wrong column", () => {
        expect(new Set(RESULT_METRICS.map((metric) => metric.id)).size).toBe(RESULT_METRICS.length)
    })

    it("reduces energy and carbon by summing intervals, not by averaging them", () => {
        const summed = RESULT_METRICS.filter((metric) => metric.reduce === "sum").map((metric) => metric.id)
        expect(summed).toContain("powerSource.energy_usage")
        expect(summed).toContain("powerSource.carbon_emission")
    })
})

describe("reduceSeries", () => {
    it("reports an empty series as having no value rather than as zero", () => {
        expect(reduceSeries([], "mean")).toEqual({ status: "empty" })
        expect(reduceSeries([], "sum")).toEqual({ status: "empty" })
        expect(reduceSeries([], "max")).toEqual({ status: "empty" })
        expect(reduceSeries([], "last")).toEqual({ status: "empty" })
    })

    it("distinguishes a genuine zero from missing data", () => {
        expect(reduceSeries(points(0, 0), "mean")).toEqual({ status: "ok", value: 0 })
    })

    it("takes the final sample for cumulative counters instead of the largest", () => {
        expect(reduceSeries(points(5, 9, 7), "last")).toEqual({ status: "ok", value: 7 })
        expect(reduceSeries(points(5, 9, 7), "max")).toEqual({ status: "ok", value: 9 })
    })

    it("averages over the samples present, not over the full simulated span", () => {
        expect(reduceSeries(points(2, 4), "mean")).toEqual({ status: "ok", value: 3 })
    })

    it("applies the reduction the metric declares", () => {
        const reported = scenario({
            series: [
                { metric: "powerSource.energy_usage" as const, points: points(10, 10, 10), spread: 0 },
                { metric: "host.cpu_utilization" as const, points: points(0.2, 0.8), spread: 0 },
            ],
        })
        expect(reduceMetric(reported, "powerSource.energy_usage")).toEqual({ status: "ok", value: 30 })
        expect(reduceMetric(reported, "host.cpu_utilization")).toEqual({ status: "ok", value: 0.5 })
    })

    it("treats a metric the run never reported as empty instead of failing", () => {
        expect(seriesOf(scenario(), "battery.charge").points).toEqual([])
        expect(reduceMetric(scenario(), "battery.charge")).toEqual({ status: "empty" })
    })
})

describe("alignOnTimestamp", () => {
    it("keeps samples from a shorter run instead of truncating every run to it", () => {
        const rows = alignOnTimestamp([points(1, 2, 3), points(4)])
        expect(rows).toHaveLength(3)
        expect(rows[0]?.values).toEqual({ 0: 1, 1: 4 })
        expect(rows[2]?.values).toEqual({ 0: 3 })
    })

    it("orders rows by simulated time even when the series arrive out of order", () => {
        const rows = alignOnTimestamp([[{ t: 600, value: 1 }], [{ t: 0, value: 2 }]])
        expect(rows.map((row) => row.t)).toEqual([0, 600])
    })

    it("has nothing to plot when no run has reported yet", () => {
        expect(alignOnTimestamp([])).toEqual([])
        expect(alignOnTimestamp([[], []])).toEqual([])
    })
})

describe("live results", () => {
    const experiment = (scenarios: ScenarioResults[], complete: boolean) => ({
        experimentId: "4d2e0c34-2b0f-4a0f-9d21-8b0d4a1c9f77",
        exportIntervalMs: EXPORT_INTERVAL_MS,
        bucketMs: EXPORT_INTERVAL_MS,
        complete,
        scenarios,
    })

    it("does not poll a draft that has no scenarios to report on", () => {
        expect(isLiveResults(experiment([], false))).toBe(false)
    })

    it("stops polling once every scenario has settled", () => {
        expect(isLiveResults(experiment([scenario({ complete: true })], true))).toBe(false)
    })

    it("keeps polling while a scenario is still producing samples", () => {
        expect(isLiveResults(experiment([scenario({ complete: false })], false))).toBe(true)
    })

    it("keeps polling while one scenario lags behind a finished sibling", () => {
        const scenarios = [scenario({ complete: true }), scenario({ scenarioIndex: 1, complete: false })]
        expect(isLiveResults(experiment(scenarios, false))).toBe(true)
    })
})
