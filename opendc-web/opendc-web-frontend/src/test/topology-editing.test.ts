import {
    type TopologyPlan,
    addCluster,
    addDataCenter,
    addHost,
    duplicateCluster,
    duplicateDataCenter,
    moveClusters,
    placeCluster,
    removeClusters,
    removeDataCenter,
    removeHost,
    updateClusters,
    updateDataCenter,
    updateHost,
} from "@/lib/topology/edits"
import {
    DEFAULT_FLOOR_WIDTH,
    LAYOUT_VERSION,
    autoPlan,
    clusterAt,
    floorPlanOf,
    freeCell,
    reconcileFloor,
    reserveCell,
} from "@/lib/topology/layout"
import type { ClusterSpec, DataCenterSpec, HostSpec, TopologySpec } from "@/lib/topology/spec"
import { describe, expect, it } from "vitest"

function host(overrides: Partial<HostSpec> = {}): HostSpec {
    return { cpu: { coreCount: 8, coreSpeed: "3 GHz" }, memory: { size: "64 GiB" }, ...overrides }
}

function cluster(name: string): ClusterSpec {
    return { name, hosts: [host()] }
}

function dataCenter(name: string, ...clusters: string[]): DataCenterSpec {
    return { name, clusters: clusters.map(cluster) }
}

function planOf(...dataCenters: DataCenterSpec[]): TopologyPlan {
    const topology: TopologySpec = { datacenters: dataCenters }
    return { topology, layout: autoPlan(topology) }
}

function names(plan: TopologyPlan, at = 0): Array<string | undefined> {
    return plan.topology.datacenters[at]?.clusters.map((entry) => entry.name) ?? []
}

function cellsOf(plan: TopologyPlan, at = 0) {
    return plan.layout.floors[at]?.cells ?? []
}

describe("data center edits", () => {
    it("keeps fields the editor does not understand, so documents round-trip", () => {
        const topology = {
            schemaVersion: 7,
            datacenters: [{ name: "a", clusters: [cluster("c")], coolingProfile: { kind: "immersion" } }],
        } as TopologySpec
        const plan = updateDataCenter({ topology, layout: autoPlan(topology) }, 0, { name: "renamed" })

        expect(plan.topology.schemaVersion).toBe(7)
        expect(plan.topology.datacenters[0]?.coolingProfile).toEqual({ kind: "immersion" })
        expect(plan.topology.datacenters[0]?.name).toBe("renamed")
    })

    it("adds a data center with a floor of its own", () => {
        const plan = addDataCenter(planOf(dataCenter("DC 1", "a")), dataCenter("DC 2"))
        expect(plan.topology.datacenters).toHaveLength(2)
        expect(plan.layout.floors).toHaveLength(2)
        expect(cellsOf(plan, 1)).toEqual([])
    })

    it("puts a duplicate right after its source, under a name of its own and on the same floor plan", () => {
        const plan = duplicateDataCenter(
            duplicateDataCenter(planOf(dataCenter("DC", "a", "b"), dataCenter("Other")), 0),
            0,
        )
        expect(plan.topology.datacenters.map((entry) => entry.name)).toEqual(["DC", "DC copy 2", "DC copy", "Other"])
        expect(cellsOf(plan, 1)).toEqual(cellsOf(plan, 0))
    })

    it("takes its floor with it when a data center is removed, so the others stay lined up", () => {
        const start = planOf(dataCenter("a", "x"), dataCenter("b", "y", "z"))
        const plan = removeDataCenter(start, 0)
        expect(plan.topology.datacenters.map((entry) => entry.name)).toEqual(["b"])
        expect(plan.layout.floors).toEqual([start.layout.floors[1]])
    })

    it("ignores edits addressed to data centers or clusters that do not exist", () => {
        const plan = planOf(dataCenter("a", "x"))
        expect(removeDataCenter(plan, 3)).toBe(plan)
        expect(duplicateDataCenter(plan, 3)).toBe(plan)
        expect(addCluster(plan, 3, cluster("y"), { x: 0, y: 0 })).toBe(plan)
        expect(duplicateCluster(plan, { dataCenter: 0, cluster: 4 })).toBe(plan)
        expect(addHost(plan, { dataCenter: 0, cluster: 4 }, host())).toBe(plan)
    })
})

describe("cluster edits", () => {
    it("removes several clusters with their cells, without earlier deletions shifting later ones", () => {
        const plan = removeClusters(planOf(dataCenter("DC", "a", "b", "c", "d")), 0, [0, 2])
        expect(names(plan)).toEqual(["b", "d"])
        expect(cellsOf(plan)).toEqual([
            { x: 1, y: 0 },
            { x: 3, y: 0 },
        ])
    })

    it("inserts a duplicate next to its source on a free cell", () => {
        const plan = duplicateCluster(planOf(dataCenter("DC", "a", "b")), { dataCenter: 0, cluster: 0 })
        expect(names(plan)).toEqual(["a", "a", "b"])
        expect(new Set(cellsOf(plan).map((cell) => `${cell.x},${cell.y}`)).size).toBe(3)
    })

    it("does not let an edit to a duplicate leak back into its source", () => {
        const duplicated = duplicateCluster(planOf(dataCenter("DC", "a")), { dataCenter: 0, cluster: 0 })
        const edited = updateHost(duplicated, { dataCenter: 0, cluster: 1, host: 0 }, { count: 16 })
        expect(edited.topology.datacenters[0]?.clusters[0]?.hosts[0]?.count).toBeUndefined()
        expect(edited.topology.datacenters[0]?.clusters[1]?.hosts[0]?.count).toBe(16)
    })

    it("swaps two clusters when one is dropped onto the other, and ignores a drop off the floor", () => {
        const plan = planOf(dataCenter("DC", "a", "b"))
        const swapped = placeCluster(plan, { dataCenter: 0, cluster: 0 }, { x: 1, y: 0 })
        expect(cellsOf(swapped)).toEqual([
            { x: 1, y: 0 },
            { x: 0, y: 0 },
        ])
        expect(placeCluster(plan, { dataCenter: 0, cluster: 0 }, { x: 0, y: 999 }).layout).toEqual(plan.layout)
    })

    it("edits only the clusters asked for, in the data center asked for", () => {
        const plan = updateClusters(planOf(dataCenter("a", "x", "y"), dataCenter("b", "x")), 0, [1], { count: 3 })
        expect(plan.topology.datacenters[0]?.clusters.map((entry) => entry.count)).toEqual([undefined, 3])
        expect(plan.topology.datacenters[1]?.clusters[0]?.count).toBeUndefined()
    })

    // A tile cannot be dropped onto another data center's tab, so moving is its own edit.
    it("moves clusters to another data center in their order, onto free cells of its floor", () => {
        const start = planOf(dataCenter("a", "x", "y", "z"), dataCenter("b", "w"))
        const plan = moveClusters(start, 0, [2, 0], 1)
        expect(names(plan, 0)).toEqual(["y"])
        expect(names(plan, 1)).toEqual(["w", "x", "z"])
        expect(cellsOf(plan, 0)).toHaveLength(1)
        expect(cellsOf(plan, 1)).toEqual([
            { x: 0, y: 0 },
            { x: 1, y: 0 },
            { x: 2, y: 0 },
        ])
    })

    it("does not move clusters onto the data center they are in, or to one that does not exist", () => {
        const plan = planOf(dataCenter("a", "x"))
        expect(moveClusters(plan, 0, [0], 0)).toBe(plan)
        expect(moveClusters(plan, 0, [0], 5)).toBe(plan)
    })
})

describe("host edits", () => {
    it("changes one host group without touching its siblings", () => {
        const plan = addHost(planOf(dataCenter("DC", "a")), { dataCenter: 0, cluster: 0 }, host({ name: "gpu" }))
        const edited = updateHost(plan, { dataCenter: 0, cluster: 0, host: 1 }, { count: 8 })
        expect(edited.topology.datacenters[0]?.clusters[0]?.hosts.map((entry) => entry.count)).toEqual([undefined, 8])
    })

    it("can empty a cluster, which validation then rejects", () => {
        const plan = removeHost(planOf(dataCenter("DC", "a")), { dataCenter: 0, cluster: 0, host: 0 })
        expect(plan.topology.datacenters[0]?.clusters[0]?.hosts).toEqual([])
    })
})

describe("floor plan", () => {
    it("lays out one floor per data center, deterministically", () => {
        const topology = { datacenters: [dataCenter("a", "x", "y"), dataCenter("b", "z")] }
        expect(autoPlan(topology)).toEqual(autoPlan(topology))
        expect(autoPlan(topology).floors.map((floor) => floor.cells.length)).toEqual([2, 1])
    })

    // The server keeps the plan opaque, so whatever an older editor or a hand edit left there has to
    // be read defensively.
    it("lays out afresh a stored plan it cannot read, or one of another version", () => {
        const topology = { datacenters: [dataCenter("a", "x")] }
        const fresh = autoPlan(topology)
        expect(floorPlanOf(undefined, topology)).toEqual(fresh)
        expect(floorPlanOf({ version: 1, size: { width: 4, height: 4 }, cells: [{ x: 3, y: 3 }] }, topology)).toEqual(
            fresh,
        )
        expect(floorPlanOf({ version: LAYOUT_VERSION, floors: "nope" }, topology)).toEqual(fresh)
    })

    it("keeps a stored arrangement, adding and dropping floors to match the data centers", () => {
        const stored = {
            version: LAYOUT_VERSION,
            floors: [{ size: { width: 4, height: 4 }, cells: [{ x: 3, y: 3 }] }, {}],
        }
        const plan = floorPlanOf(stored, { datacenters: [dataCenter("a", "x"), dataCenter("b", "y")] })
        expect(plan).toEqual(autoPlan({ datacenters: [dataCenter("a", "x"), dataCenter("b", "y")] }))

        const valid = { version: LAYOUT_VERSION, floors: [{ size: { width: 4, height: 4 }, cells: [{ x: 3, y: 3 }] }] }
        const reconciled = floorPlanOf(valid, { datacenters: [dataCenter("a", "x"), dataCenter("b", "y")] })
        expect(reconciled.floors[0]?.cells).toEqual([{ x: 3, y: 3 }])
        expect(reconciled.floors[1]?.cells).toEqual([{ x: 0, y: 0 }])
    })

    it("packs clusters row major and wraps at the floor width", () => {
        const clusters = Array.from({ length: DEFAULT_FLOOR_WIDTH + 1 }, (_, index) => `c${index}`)
        const floor = autoPlan({ datacenters: [dataCenter("a", ...clusters)] }).floors[0]
        expect(floor?.cells[DEFAULT_FLOOR_WIDTH - 1]).toEqual({ x: DEFAULT_FLOOR_WIDTH - 1, y: 0 })
        expect(floor?.cells[DEFAULT_FLOOR_WIDTH]).toEqual({ x: 0, y: 1 })
    })

    it("separates clusters a corrupted floor stacked on one cell, and grows a floor too small", () => {
        const stacked = reconcileFloor(
            {
                size: { width: 4, height: 4 },
                cells: [
                    { x: 2, y: 2 },
                    { x: 2, y: 2 },
                ],
            },
            2,
        )
        expect(stacked.cells[0]).toEqual({ x: 2, y: 2 })
        expect(stacked.cells[1]).not.toEqual({ x: 2, y: 2 })
        const grown = reconcileFloor({ size: { width: 2, height: 1 }, cells: [] }, 3)
        expect(grown.size.height).toBe(2)
        expect(grown.cells).toHaveLength(3)
    })

    it("grows the floor rather than failing when claiming a cell on a full one", () => {
        const full = { size: { width: 1, height: 1 }, cells: [{ x: 0, y: 0 }] }
        expect(reserveCell(full, { x: 0, y: 0 })).toEqual({
            floor: { ...full, size: { width: 1, height: 2 } },
            cell: { x: 0, y: 1 },
        })
        expect(freeCell(full).cell).toEqual({ x: 0, y: 1 })
    })

    it("reports what occupies a cell", () => {
        const floor = autoPlan({ datacenters: [dataCenter("a", "x", "y")] }).floors[0]
        if (!floor) throw new Error("no floor")
        expect(clusterAt(floor, { x: 1, y: 0 })).toEqual({ status: "occupied", cluster: 1 })
        expect(clusterAt(floor, { x: 9, y: 9 })).toEqual({ status: "empty" })
    })
})
