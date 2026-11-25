import { bindFailureModels, bindSchedulers, bindTopologies, bindWorkloads } from "@/components/experiment/draftAxes"
import type { CatalogEntry, TopologyTemplate } from "@/lib/api/types"
import type { AllocationPolicySpec, FailureModelSpec, WorkloadSpec } from "@/lib/experiment/spec"
import type { TopologySpec } from "@/lib/topology/spec"
import { describe, expect, it } from "vitest"

function catalog(...ids: string[]): CatalogEntry[] {
    return ids.map((id) => ({ id, label: id, group: "test", description: `about ${id}` }))
}

function topology(name: string): TopologySpec {
    const hosts = [{ cpu: { coreCount: 8, coreSpeed: "3 GHz" }, memory: { size: "64 GiB" } }]
    return { datacenters: [{ name: "DC", clusters: [{ name, hosts }] }] }
}

function template(id: string, name: string): TopologyTemplate {
    const spec = topology(name)
    return {
        id,
        projectId: "project-1",
        name,
        topology: spec,
        // Deliberately not derived from the document: matching is by content, so a hash the client
        // could never reproduce must not be what makes these tests pass.
        topologyHash: `server-hash-${id}`,
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
    }
}

const TEMPLATES = [template("baseline-id", "Production baseline"), template("sandbox-id", "Edge sandbox")]

const named = (name: string): WorkloadSpec => ({ type: "trace", source: { type: "named", name } })

describe("topology axis", () => {
    it("selects the template a spec topology came from, by content and not by position", () => {
        const bound = bindTopologies([topology("Edge sandbox")], TEMPLATES)
        expect(bound.selected).toEqual(["sandbox-id"])
    })

    it("rebuilds the spec from the chosen templates, in the order they were chosen", () => {
        const bound = bindTopologies([topology("Production baseline")], TEMPLATES)
        expect(bound.rebuild(["sandbox-id", "baseline-id"])).toEqual([
            topology("Edge sandbox"),
            topology("Production baseline"),
        ])
    })

    it("keeps a topology that no longer matches any template instead of dropping it", () => {
        const orphan = topology("deleted-cluster")
        const bound = bindTopologies([orphan, topology("Edge sandbox")], TEMPLATES)

        expect(bound.selected).toHaveLength(2)
        expect(bound.rebuild(bound.selected)).toEqual([orphan, topology("Edge sandbox")])
        expect(bound.choices.some((choice) => choice.label.includes("no longer a saved topology"))).toBe(true)
    })

    it("has nothing selected when the project has no topologies at all", () => {
        const bound = bindTopologies([], [])

        expect(bound.selected).toEqual([])
        expect(bound.rebuild([])).toEqual([])
    })
})

describe("workload axis", () => {
    it("round-trips a named trace without rewriting it", () => {
        const bound = bindWorkloads([named("azure-2019")], catalog("bitbrains-small", "azure-2019"))

        expect(bound.selected).toEqual(["azure-2019"])
        expect(bound.rebuild(bound.selected)).toEqual([named("azure-2019")])
    })

    it("keeps an inline workload the editor cannot name", () => {
        const inline: WorkloadSpec = { type: "inline", tasks: [{ id: "a" }] }
        const bound = bindWorkloads([inline, named("azure-2019")], catalog("azure-2019"))

        expect(bound.rebuild(bound.selected)).toEqual([inline, named("azure-2019")])
    })

    it("preserves fields the editor does not render, such as a sample fraction", () => {
        const sampled: WorkloadSpec = {
            type: "trace",
            source: { type: "named", name: "azure-2019" },
            sampleFraction: 0.25,
        }
        const bound = bindWorkloads([sampled], catalog("azure-2019", "bitbrains-small"))

        expect(bound.selected).toEqual(["azure-2019"])
        expect(bound.rebuild(["azure-2019", "bitbrains-small"])).toEqual([sampled, named("bitbrains-small")])
    })

    it("keeps an efficient trace as its own choice instead of rewriting it as a plain trace", () => {
        const efficient: WorkloadSpec = { type: "efficient-trace", source: { type: "named", name: "azure-2019" } }
        const bound = bindWorkloads([efficient], catalog("bitbrains-small"))

        expect(bound.selected).not.toContain("azure-2019")
        expect(bound.choices.map((choice) => choice.label)).toContain("azure-2019")
        expect(bound.rebuild(bound.selected)).toEqual([efficient])
    })

    it("offers a description for every catalog entry so the picker can explain itself", () => {
        const bound = bindWorkloads([], catalog("azure-2019"))
        expect(bound.choices[0]?.description).toBe("about azure-2019")
    })
})

describe("scheduler axis", () => {
    it("names a prefab policy by its prefab", () => {
        const bound = bindSchedulers([{ type: "prefab", prefabName: "CoreMem" }], catalog("Mem", "CoreMem"))
        expect(bound.selected).toEqual(["CoreMem"])
    })

    it("treats a prefab policy without a name as the SDK default rather than as unnamed", () => {
        const bound = bindSchedulers([{ type: "prefab" }], catalog("Mem"))
        expect(bound.selected).toEqual(["Mem"])
    })

    it("keeps a hand written filter policy that has no prefab name", () => {
        const custom: AllocationPolicySpec = { type: "filter", filters: [{ type: "compute" }] }
        const bound = bindSchedulers([custom], catalog("Mem"))

        expect(bound.rebuild(bound.selected)).toEqual([custom])
    })
})

describe("failure axis", () => {
    it("offers the absence of failures as a choice of its own", () => {
        const bound = bindFailureModels([{ type: "none" }], catalog("G5k06Exp"))

        expect(bound.selected).toEqual(["none"])
        expect(bound.rebuild(["none"])).toEqual([{ type: "none" }])
    })

    it("keeps a trace based failure model that no catalog entry can express", () => {
        const traced: FailureModelSpec = { type: "trace", source: { type: "uri", uri: "s3://failures" } }
        const bound = bindFailureModels([traced], catalog("G5k06Exp"))

        expect(bound.rebuild(bound.selected)).toEqual([traced])
        expect(bound.choices.map((choice) => choice.label)).toContain("Failures from a trace")
    })

    it("drops an entry whose choice disappeared rather than inventing one", () => {
        const bound = bindFailureModels([{ type: "none" }], catalog("G5k06Exp"))
        expect(bound.rebuild([])).toEqual([])
    })
})
