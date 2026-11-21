import { topologyLabel } from "@/components/experiment/axisLabels"
import type { CatalogEntry, TopologyTemplate } from "@/lib/api/types"
import {
    type AllocationPolicySpec,
    DEFAULT_SCHEDULER,
    type FailureModelSpec,
    type WorkloadSpec,
} from "@/lib/experiment/spec"
import type { TopologySpec } from "@/lib/topology/spec"

export interface AxisChoice {
    value: string
    label: string
    description?: string
}

export interface AxisBinding<T> {
    selected: string[]
    choices: AxisChoice[]
    rebuild: (values: string[]) => T[]
}

const KEPT = "kept:"

// Entries the editor has no name for are kept as their own choice, so an edit never drops them.
function bindAxis<T>(
    entries: T[],
    nameOf: (entry: T) => string | undefined,
    labelOf: (entry: T) => string,
    make: (name: string) => T | undefined,
    catalog: AxisChoice[],
): AxisBinding<T> {
    const kept = new Map<string, T>()
    const selected = entries.map((entry, index) => {
        const name = nameOf(entry)
        if (name !== undefined) return name
        const key = `${KEPT}${index}`
        kept.set(key, entry)
        return key
    })

    return {
        selected,
        choices: [...catalog, ...[...kept].map(([value, entry]) => ({ value, label: labelOf(entry) }))],
        rebuild: (values) =>
            values.flatMap((value) => {
                const entry = kept.get(value) ?? make(value)
                return entry === undefined ? [] : [entry]
            }),
    }
}

export function catalogChoices(entries: CatalogEntry[]): AxisChoice[] {
    return entries.map((entry) => ({
        value: entry.id,
        label: entry.label,
        ...(entry.description ? { description: entry.description } : {}),
    }))
}

// Experiments copy their topology, so it is matched to a template by content. Both arrive in the
// server's canonical field order, so stringifying compares soundly; a mismatch only shows it unsaved.
function documentKey(topology: TopologySpec): string {
    return JSON.stringify(topology)
}

export function bindTopologies(entries: TopologySpec[], templates: TopologyTemplate[]): AxisBinding<TopologySpec> {
    const byId = new Map(templates.map((template) => [String(template.id), template]))
    const byContent = new Map(templates.map((template) => [documentKey(template.topology), String(template.id)]))

    return bindAxis(
        entries,
        (topology) => byContent.get(documentKey(topology)),
        (topology) => `${topologyLabel(topology)}, no longer a saved topology`,
        (value) => byId.get(value)?.topology,
        templates.map((template) => ({
            value: String(template.id),
            label: template.name,
            description: topologyLabel(template.topology),
        })),
    )
}

export function bindWorkloads(entries: WorkloadSpec[], traces: CatalogEntry[]): AxisBinding<WorkloadSpec> {
    return bindAxis(
        entries,
        (workload) =>
            workload.type === "trace" && workload.source.type === "named" ? workload.source.name : undefined,
        (workload) => (workload.type === "inline" ? "Inline task list" : "Workload from a URI"),
        (value) => ({ type: "trace", source: { type: "named", name: value } }),
        catalogChoices(traces),
    )
}

export function bindSchedulers(
    entries: AllocationPolicySpec[],
    schedulers: CatalogEntry[],
): AxisBinding<AllocationPolicySpec> {
    return bindAxis(
        entries,
        (policy) => (policy.type === "prefab" ? (policy.prefabName ?? DEFAULT_SCHEDULER) : undefined),
        (policy) => `Custom ${policy.type} policy`,
        (value) => ({ type: "prefab", prefabName: value }),
        catalogChoices(schedulers),
    )
}

const NO_FAILURES = "none"

export function bindFailureModels(entries: FailureModelSpec[], prefabs: CatalogEntry[]): AxisBinding<FailureModelSpec> {
    return bindAxis(
        entries,
        (model) => (model.type === "none" ? NO_FAILURES : model.type === "prefab" ? model.prefabName : undefined),
        (model) => (model.type === "traceBased" ? "Failures from a trace" : "Custom failure model"),
        (value) => (value === NO_FAILURES ? { type: "none" } : { type: "prefab", prefabName: value }),
        [
            {
                value: NO_FAILURES,
                label: "No failures",
                description: "Hosts never fail, so the run measures scheduling alone.",
            },
            ...catalogChoices(prefabs),
        ],
    )
}
