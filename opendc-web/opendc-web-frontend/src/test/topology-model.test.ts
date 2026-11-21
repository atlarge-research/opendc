import { starterPowerModel } from "@/components/topology/defaults"
import {
    UNLIMITED_SUPPLY_W,
    clusterShare,
    dataCenterHeadroom,
    isOverBudget,
    overBudgetDataCenters,
    suggestedSupplyW,
    supplyOf,
    topologyCapacity,
} from "@/lib/topology/capacity"
import type { ClusterSpec, DataCenterSpec, HostSpec, PowerSpec, TopologySpec } from "@/lib/topology/spec"
import { brokenClusters, brokenDataCenters, issuesUnder, validateTopology } from "@/lib/topology/validation"
import { describe, expect, it } from "vitest"

function host(overrides: Partial<HostSpec> = {}): HostSpec {
    return {
        cpu: { coreCount: 32, coreSpeed: "3.2 GHz" },
        memory: { size: "128 GiB" },
        cpuPowerModel: { type: "linear", maxPower: "400 Watts", idlePower: "120 Watts" },
        ...overrides,
    }
}

function cluster(overrides: Partial<ClusterSpec> = {}): ClusterSpec {
    return { name: "compute", hosts: [host()], ...overrides }
}

function dataCenter(clusters: ClusterSpec[], overrides: Partial<DataCenterSpec> = {}): DataCenterSpec {
    return { name: "DC", clusters, ...overrides }
}

function topology(clusters: ClusterSpec[]): TopologySpec {
    return { datacenters: [dataCenter(clusters)] }
}

function paths(spec: TopologySpec): string[] {
    return validateTopology(spec).map((issue) => issue.path)
}

describe("validateTopology", () => {
    it("accepts a well formed topology", () => {
        expect(validateTopology(topology([cluster()]))).toEqual([])
    })

    it("rejects a topology with no data centers, and a data center with no clusters", () => {
        expect(validateTopology({ datacenters: [] })).toEqual([{ path: "datacenters", message: "must not be empty" }])
        expect(validateTopology(topology([]))).toEqual([
            { path: "datacenters[0].clusters", message: "must not be empty" },
        ])
    })

    it("rejects a cluster with no hosts", () => {
        expect(validateTopology(topology([cluster({ hosts: [] })]))).toContainEqual({
            path: "datacenters[0].clusters[0].hosts",
            message: "must not be empty",
        })
    })

    it("reports the paths the simulator's own validation reports, through every data center", () => {
        const broken = cluster({ hosts: [host(), host({ cpu: { coreCount: 0, coreSpeed: 1 } })] })
        const spec = { datacenters: [dataCenter([cluster()]), dataCenter([cluster(), broken])] }
        expect(paths(spec)).toEqual(["datacenters[1].clusters[1].hosts[1].cpu.coreCount"])
    })

    it("rejects non positive counts", () => {
        expect(paths(topology([cluster({ hosts: [host({ count: 0 })] })]))).toContain(
            "datacenters[0].clusters[0].hosts[0].count",
        )
    })

    it("rejects a power model whose peak is below its idle draw", () => {
        const spec = topology([
            cluster({ hosts: [host({ cpuPowerModel: { type: "sqrt", maxPower: "100 W", idlePower: "200 W" } })] }),
        ])
        expect(validateTopology(spec)).toContainEqual({
            path: "datacenters[0].clusters[0].hosts[0].cpuPowerModel.maxPower",
            message: "must be >= idlePower",
        })
    })

    it("checks a constant model's one draw and a fitted model's own parameter", () => {
        const constant = host({ cpuPowerModel: { type: "constant", power: "a lot" } })
        const mse = host({ cpuPowerModel: { type: "mse", maxPower: "200 W", idlePower: "50 W", calibrationFactor: 0 } })
        expect(paths(topology([cluster({ hosts: [constant, mse] })]))).toEqual([
            "datacenters[0].clusters[0].hosts[0].cpuPowerModel.power",
            "datacenters[0].clusters[0].hosts[1].cpuPowerModel.calibrationFactor",
        ])
    })

    it("only validates the gpu power model when the host actually has a gpu", () => {
        const broken = { type: "linear" as const, maxPower: "10 W", idlePower: "90 W" }
        expect(validateTopology(topology([cluster({ hosts: [host({ gpuPowerModel: broken })] })]))).toEqual([])
        const withGpu = topology([
            cluster({ hosts: [host({ gpu: { coreCount: 6912, coreSpeed: "1.4 GHz" }, gpuPowerModel: broken })] }),
        ])
        expect(paths(withGpu)).toContain("datacenters[0].clusters[0].hosts[0].gpuPowerModel.maxPower")
    })

    it("rejects quantities the backend cannot deserialize, the supply limit included", () => {
        const spec = {
            datacenters: [
                dataCenter([cluster({ hosts: [host({ cpu: { coreCount: 4, coreSpeed: "3.2 parsecs" } })] })], {
                    powerSource: { maxPower: "lots" },
                }),
            ],
        }
        expect(paths(spec)).toEqual([
            "datacenters[0].clusters[0].hosts[0].cpu.coreSpeed",
            "datacenters[0].powerSource.maxPower",
        ])
    })

    it("rejects a negative measurement where the backend has no unset sentinel", () => {
        expect(paths(topology([cluster({ hosts: [host({ memory: { size: -1 } })] })]))).toContain(
            "datacenters[0].clusters[0].hosts[0].memory.size",
        )
    })
})

describe("issues", () => {
    it("does not confuse a prefix with a longer index sharing its digits", () => {
        const issues = [
            { path: "datacenters[1].clusters", message: "a" },
            { path: "datacenters[10].clusters", message: "b" },
            { path: "datacenters[1]", message: "c" },
        ]
        expect(issuesUnder(issues, "datacenters[1]").map((issue) => issue.message)).toEqual(["a", "c"])
    })

    it("marks the data centers and clusters with a problem anywhere inside them", () => {
        const issues = [
            { path: "datacenters[1].clusters[2].hosts[0].count", message: "must be > 0" },
            { path: "datacenters[0].powerSource.maxPower", message: "must be a valid power" },
        ]
        expect(brokenDataCenters(issues, 3)).toEqual([0, 1])
        expect(brokenClusters(issues, 1, 3)).toEqual([2])
        expect(brokenClusters(issues, 0, 3)).toEqual([])
    })
})

describe("topologyCapacity", () => {
    it("multiplies cluster count, host count and cpu count together, across data centers", () => {
        const big = cluster({
            count: 3,
            hosts: [host({ count: 4, cpu: { coreCount: 32, coreSpeed: "3 GHz", count: 2 } })],
        })
        const capacity = topologyCapacity({ datacenters: [dataCenter([big]), dataCenter([cluster()])] })
        expect(capacity.dataCenters).toBe(2)
        expect(capacity.hosts).toBe(12 + 1)
        expect(capacity.cores).toBe(3 * 4 * 2 * 32 + 32)
    })

    it("sums memory across host groups in MiB", () => {
        const spec = topology([cluster({ hosts: [host({ count: 2 }), host({ memory: { size: "256 GiB" } })] })])
        expect(topologyCapacity(spec).memoryMiB).toBe(2 * 131_072 + 262_144)
    })

    it("excludes the gpu power model from peak draw when the host has no gpu", () => {
        const gpuPowerModel = { type: "linear" as const, maxPower: "400 W", idlePower: "100 W" }
        expect(topologyCapacity(topology([cluster({ hosts: [host({ gpuPowerModel })] })])).peakPowerW).toBe(400)
        const withGpu = topology([cluster({ hosts: [host({ gpu: { coreCount: 1, coreSpeed: 1 }, gpuPowerModel })] })])
        expect(topologyCapacity(withGpu).peakPowerW).toBe(800)
    })

    it("counts a constant model's draw as its peak", () => {
        const spec = topology([cluster({ hosts: [host({ cpuPowerModel: { type: "constant", power: "250 W" } })] })])
        expect(topologyCapacity(spec).peakPowerW).toBe(250)
    })
})

describe("starterPowerModel", () => {
    const linear: PowerSpec = { type: "linear", maxPower: "300 W", idlePower: "90 W" }

    it("keeps the draw across a change of type and carries only the new type's fields", () => {
        const constant = starterPowerModel("constant", linear)
        expect(constant).toEqual({ type: "constant", power: "300 W" })
        expect(starterPowerModel("cubic", constant)).toEqual({ type: "cubic", maxPower: "300 W", idlePower: "300 W" })
    })

    it("gives a fitted model a parameter the server accepts", () => {
        expect(starterPowerModel("mse", linear)).toEqual({ ...linear, type: "mse", calibrationFactor: 1 })
        const asymptotic = starterPowerModel("asymptotic", starterPowerModel("mse", linear))
        expect(asymptotic).toEqual({ ...linear, type: "asymptotic", asymUtil: 0.3 })
        expect(validateTopology(topology([cluster({ hosts: [host({ cpuPowerModel: asymptotic })] })]))).toEqual([])
    })
})

describe("supply", () => {
    // The server writes the simulator's default supply back out in full, as Long.MAX_VALUE watts.
    it("reads an absent, unreadable or simulator-default supply as unlimited rather than as a limit", () => {
        expect(supplyOf(dataCenter([]))).toEqual({ status: "unlimited" })
        expect(supplyOf(dataCenter([], { powerSource: { maxPower: "lots" } }))).toEqual({ status: "unlimited" })
        const stored = dataCenter([], { powerSource: { maxPower: "9223372036854776.000000 KWatts" } })
        expect(supplyOf(stored)).toEqual({ status: "unlimited" })
        expect(UNLIMITED_SUPPLY_W).toBe(2 ** 63)
    })

    it("shares one supply between a data center's clusters, never scaled by their copies", () => {
        const copies = cluster({ count: 2, hosts: [host({ count: 2 })] })
        const spec = dataCenter([copies, cluster()], { powerSource: { maxPower: "2 kW" } })
        expect(dataCenterHeadroom(spec)).toEqual({
            usedW: 2 * 2 * 400 + 400,
            budget: { status: "limited", watts: 2000 },
        })
        expect(isOverBudget(dataCenterHeadroom(spec))).toBe(false)
        expect(clusterShare(copies, supplyOf(spec))).toEqual({ status: "limited", drawW: 1600, fraction: 0.8 })
    })

    it("flags the data centers drawing more than their supply, and only those", () => {
        const hungry = cluster({ hosts: [host({ count: 10 })] })
        const spec = {
            datacenters: [
                dataCenter([hungry], { powerSource: { maxPower: "1 kW" } }),
                dataCenter([hungry]),
                dataCenter([cluster()], { powerSource: { maxPower: "1 kW" } }),
            ],
        }
        expect(overBudgetDataCenters(spec)).toEqual([0])
    })

    it("suggests the peak rounded up to a whole kilowatt, and never less than one", () => {
        expect(suggestedSupplyW(dataCenter([cluster({ hosts: [host({ count: 3 })] })]))).toBe(2000)
        expect(suggestedSupplyW(dataCenter([]))).toBe(1000)
    })
})
