import {
    AXIS_ORDER,
    type ExperimentSpec,
    experimentAxes,
    scenarioCoordinates,
    scenarioCount,
} from "@/lib/experiment/spec"
import {
    type ExperimentState,
    type ScenarioExecutionState,
    foldExperimentState,
    isTerminalExperiment,
    progressFraction,
} from "@/lib/experiment/status"
import type { TopologySpec } from "@/lib/topology/spec"
import { describe, expect, it } from "vitest"

function topology(name: string): TopologySpec {
    const hosts = [{ cpu: { coreCount: 8, coreSpeed: "3 GHz" }, memory: { size: "64 GiB" } }]
    return { datacenters: [{ name: "DC", clusters: [{ name, hosts }] }] }
}

function trace(name: string) {
    return { type: "trace", source: { type: "named", name } } as const
}

function spec(overrides: Partial<ExperimentSpec> = {}): ExperimentSpec {
    return { topologies: [topology("a")], workloads: [trace("bitbrains-small")], ...overrides }
}

describe("scenarioCount", () => {
    it("treats an omitted axis as the single SDK default, not as an empty set", () => {
        expect(scenarioCount(spec())).toBe(1)
    })

    it("multiplies every axis together", () => {
        const wide = spec({
            topologies: [topology("a"), topology("b")],
            workloads: [trace("x"), trace("y"), trace("z")],
            allocationPolicies: [
                { type: "prefab", prefabName: "Mem" },
                { type: "prefab", prefabName: "CoreMem" },
            ],
            maxNumFailures: [5, 10],
        })
        expect(scenarioCount(wide)).toBe(2 * 3 * 2 * 2)
    })

    it("collapses to nothing when an axis is explicitly empty", () => {
        expect(scenarioCount(spec({ workloads: [] }))).toBe(0)
    })

    it("does not multiply by runs, which repeat inside a scenario", () => {
        expect(scenarioCount(spec({ runs: 8 }))).toBe(1)
    })
})

// The server flattens scenarios with the same mixed-radix rule (Cartesian.expand in the SDK) and
// files results under the flattened index. If the two orderings ever disagree, every chart is
// attributed to the wrong scenario, so the ordering is pinned here.
describe("scenarioCoordinates", () => {
    it("varies axes from least to most significant exactly as the backend does", () => {
        const axes = experimentAxes(
            spec({
                topologies: [topology("t0"), topology("t1")],
                workloads: [trace("w0"), trace("w1")],
                maxNumFailures: [1, 2],
            }),
        )

        const shape = Array.from({ length: 8 }, (_, index) => {
            const at = scenarioCoordinates(axes, index)
            return [at.topologies, at.workloads, at.maxNumFailures]
        })

        expect(shape).toEqual([
            [0, 0, 0],
            [0, 0, 1],
            [0, 1, 0],
            [0, 1, 1],
            [1, 0, 0],
            [1, 0, 1],
            [1, 1, 0],
            [1, 1, 1],
        ])
    })

    it("leaves a single-entry axis pinned at its only value", () => {
        const axes = experimentAxes(spec({ maxNumFailures: [1, 2, 3] }))
        const singleton = AXIS_ORDER.filter((key) => axes[key].length === 1)

        for (const index of [0, 1, 2]) {
            const at = scenarioCoordinates(axes, index)
            expect(singleton.every((key) => at[key] === 0)).toBe(true)
            expect(at.maxNumFailures).toBe(index)
        }
    })
})

describe("foldExperimentState", () => {
    const fold = (...states: ScenarioExecutionState[]) => foldExperimentState(states)

    it("reports an experiment with no scenarios as a draft rather than as running", () => {
        expect(fold()).toBe("draft")
    })

    it("stays queued only while nothing has started", () => {
        expect(fold("queued", "queued")).toBe("queued")
        expect(fold("queued", "running")).toBe("running")
        expect(fold("queued", "succeeded")).toBe("running")
    })

    it("reports success only when every scenario succeeded", () => {
        expect(fold("succeeded", "succeeded")).toBe("succeeded")
    })

    it("reports partial when a failed scenario sits beside a successful one", () => {
        expect(fold("succeeded", "failed")).toBe("partial")
        expect(fold("succeeded", "cancelled")).toBe("partial")
    })

    it("does not treat a failure as cancelling its siblings", () => {
        expect(fold("failed", "running")).toBe("running")
    })

    it("reports failure when everything terminal failed, even mixed with cancellations", () => {
        expect(fold("failed", "failed")).toBe("failed")
        expect(fold("failed", "cancelled")).toBe("failed")
    })

    it("reports cancellation only when nothing else happened", () => {
        expect(fold("cancelled", "cancelled")).toBe("cancelled")
    })

    it("marks every settled outcome as terminal", () => {
        const settled: ExperimentState[] = ["succeeded", "partial", "failed", "cancelled"]
        const live: ExperimentState[] = ["draft", "queued", "running"]
        expect(settled.every(isTerminalExperiment)).toBe(true)
        expect(live.some(isTerminalExperiment)).toBe(false)
    })
})

describe("progress", () => {
    it("reports no progress rather than dividing by zero when no task total is known yet", () => {
        expect(progressFraction({ completedTasks: 0, totalTasks: 0 })).toBe(0)
    })

    it("clamps a runner that over reports completed tasks", () => {
        expect(progressFraction({ completedTasks: 25, totalTasks: 10 })).toBe(1)
    })
})
