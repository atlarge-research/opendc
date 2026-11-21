import { canRedo, canUndo, initialHistory, record, redo, undo } from "@/components/topology/history"
import {
    type BuilderView,
    WHOLE_TOPOLOGY,
    afterClusterRemoval,
    afterDataCenterRemoval,
    clampToTopology,
    extendToCluster,
    focus,
    selectCluster,
    selectDataCenter,
    selectHost,
    selectedClusters,
    toggleHost,
} from "@/components/topology/selection"
import { describe, expect, it } from "vitest"

const cluster = (dataCenter: number, index: number) => ({ dataCenter, cluster: index })
const host = (dataCenter: number, index: number, at: number) => ({ dataCenter, cluster: index, host: at })
const viewOf = (floor: number, selection = WHOLE_TOPOLOGY): BuilderView => ({ floor, selection })

describe("selection", () => {
    it("treats a selected host as also selecting its cluster, in its own data center only", () => {
        expect(selectedClusters(selectHost(host(1, 2, 1)), 1)).toEqual([2])
        expect(selectedClusters(selectHost(host(1, 2, 1)), 0)).toEqual([])
    })

    // Reopening the group that is already open has to close it, or the inspector traps the reader
    // in a host with no way back to the cluster except Escape, which drops the cluster too.
    it("closes the open host group when it is clicked again, keeping its cluster selected", () => {
        expect(toggleHost(selectHost(host(0, 2, 1)), host(0, 2, 1))).toEqual(selectCluster(cluster(0, 2)))
        expect(toggleHost(selectCluster(cluster(0, 2)), host(0, 2, 1))).toEqual(selectHost(host(0, 2, 1)))
        expect(toggleHost(selectHost(host(0, 2, 1)), host(1, 2, 1))).toEqual(selectHost(host(1, 2, 1)))
    })

    it("adds and removes clusters when shift clicking, falling back to their data center", () => {
        const two = extendToCluster(selectCluster(cluster(0, 1)), cluster(0, 3))
        expect(selectedClusters(two, 0)).toEqual([1, 3])
        expect(selectedClusters(extendToCluster(two, cluster(0, 1)), 0)).toEqual([3])
        expect(extendToCluster(selectCluster(cluster(0, 1)), cluster(0, 1))).toEqual(selectDataCenter(0))
    })

    it("starts a new selection when shift clicking a cluster of another data center", () => {
        const selection = extendToCluster(selectCluster(cluster(0, 1)), cluster(1, 4))
        expect(selection).toEqual(selectCluster(cluster(1, 4)))
    })

    it("keeps multi-selection ordered so bulk edits are deterministic", () => {
        const selection = extendToCluster(extendToCluster(selectCluster(cluster(0, 5)), cluster(0, 2)), cluster(0, 9))
        expect(selectedClusters(selection, 0)).toEqual([2, 5, 9])
    })
})

describe("builder view", () => {
    it("shows the floor of whatever is selected, and keeps it when the whole topology is", () => {
        expect(focus(viewOf(0), selectCluster(cluster(2, 0))).floor).toBe(2)
        expect(focus(viewOf(2), WHOLE_TOPOLOGY)).toEqual(viewOf(2))
    })

    it("shifts surviving clusters down after a deletion instead of pointing at the wrong one", () => {
        const view = viewOf(0, extendToCluster(selectCluster(cluster(0, 1)), cluster(0, 4)))
        expect(selectedClusters(afterClusterRemoval(view, 0, [0, 2]).selection, 0)).toEqual([0, 2])
        expect(afterClusterRemoval(view, 1, [0])).toBe(view)
    })

    it("falls back to the data center when the selected cluster is the one deleted", () => {
        expect(afterClusterRemoval(viewOf(0, selectCluster(cluster(0, 2))), 0, [2]).selection).toEqual(
            selectDataCenter(0),
        )
        expect(afterClusterRemoval(viewOf(0, selectHost(host(0, 2, 0))), 0, [2]).selection).toEqual(selectDataCenter(0))
        expect(afterClusterRemoval(viewOf(0, selectHost(host(0, 3, 1))), 0, [0]).selection).toEqual(
            selectHost(host(0, 2, 1)),
        )
    })

    it("follows the data centers after one is removed, and steps back off a removed floor", () => {
        expect(afterDataCenterRemoval(viewOf(2, selectCluster(cluster(2, 1))), 0)).toEqual(
            viewOf(1, selectCluster(cluster(1, 1))),
        )
        expect(afterDataCenterRemoval(viewOf(1, selectDataCenter(1)), 1)).toEqual(viewOf(0))
        expect(afterDataCenterRemoval(viewOf(0, selectDataCenter(0)), 0)).toEqual(viewOf(0))
    })

    it("drops what an undo or import left pointing past the end of the topology", () => {
        const topology = {
            datacenters: [{ clusters: [{ hosts: [{ cpu: { coreCount: 1, coreSpeed: 1 }, memory: { size: 1 } }] }] }],
        }
        expect(clampToTopology(viewOf(3, selectCluster(cluster(3, 0))), topology)).toEqual(viewOf(0))
        expect(clampToTopology(viewOf(0, selectCluster(cluster(0, 7))), topology).selection).toEqual(
            selectDataCenter(0),
        )
        expect(clampToTopology(viewOf(0, selectHost(host(0, 0, 5))), topology).selection).toEqual(
            selectCluster(cluster(0, 0)),
        )
        expect(clampToTopology(viewOf(0, selectHost(host(0, 0, 0))), topology).selection).toEqual(
            selectHost(host(0, 0, 0)),
        )
    })
})

describe("history", () => {
    const start = initialHistory("a")

    it("has nothing to undo or redo when it starts", () => {
        expect(canUndo(start)).toBe(false)
        expect(canRedo(start)).toBe(false)
        expect(undo(start)).toBe(start)
        expect(redo(start)).toBe(start)
    })

    it("walks backwards and forwards through recorded edits", () => {
        const edited = record(record(start, "b"), "c")
        const back = undo(edited)
        expect(back.present).toBe("b")
        expect(redo(back).present).toBe("c")
        expect(undo(back).present).toBe("a")
    })

    it("does not record an edit that changed nothing", () => {
        expect(record(start, "a")).toBe(start)
        expect(canUndo(record(start, "a"))).toBe(false)
    })

    it("discards the redo branch once a new edit is made after undoing", () => {
        const edited = record(record(start, "b"), "c")
        const branched = record(undo(edited), "d")
        expect(canRedo(branched)).toBe(false)
        expect(branched.present).toBe("d")
        expect(undo(branched).present).toBe("b")
    })

    it("forgets the oldest edits rather than growing without bound", () => {
        let history = start
        for (let step = 0; step < 200; step++) history = record(history, `step-${step}`)
        expect(history.past.length).toBeLessThanOrEqual(50)
        expect(history.present).toBe("step-199")
    })
})
