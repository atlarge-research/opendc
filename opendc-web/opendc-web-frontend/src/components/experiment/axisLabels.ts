import { formatCount } from "@/components/format"
import type {
    AllocationPolicySpec,
    AxisKey,
    CheckpointSpec,
    ExperimentAxes,
    ExportSpec,
    FailureModelSpec,
    WorkloadSpec,
} from "@/lib/experiment/spec"
import { topologyCapacity } from "@/lib/topology/capacity"
import { type ResourceReference, type TopologySpec, dataCenterName } from "@/lib/topology/spec"
import { formatWire } from "@/lib/units"

export const AXIS_LABELS: Record<AxisKey, string> = {
    topologies: "Topology",
    workloads: "Workload",
    allocationPolicies: "Scheduler",
    failureModels: "Failure model",
    maxNumFailures: "Failure budget",
    checkpointModels: "Checkpointing",
    exportModels: "Export",
}

export const RUNS_LABEL = "Runs per scenario"
export const RUNS_HELP =
    "Repeats each scenario with a different random seed and averages the results. Raise it when one run is too noisy to trust, remembering that it multiplies the work like every other choice."

// Axes picked from a catalog; the hand-written ones get a table row only once they vary.
export const PRIMARY_AXES = ["topologies", "workloads", "allocationPolicies", "failureModels"] as const

export const AXIS_HELP: Record<AxisKey, string> = {
    topologies:
        "The datacenter itself: its clusters, its hosts, and what they draw. An experiment keeps its own copy of the topology it ran, so editing the saved one afterwards never rewrites finished results.",
    workloads:
        "The tasks arriving at the datacenter, read from a workload trace. Add a second workload to see how one datacenter copes with different demand.",
    allocationPolicies:
        "The policy that picks a host for each task. Comparing two schedulers over the same topology and workload is the most common reason to widen an axis.",
    failureModels:
        'Whether hosts break down during the run, and how often. Leave it on "no failures" to measure scheduling on its own.',
    maxNumFailures:
        "How many failures a single task survives before the simulator terminates it. It only bites once a failure model other than none is selected.",
    checkpointModels:
        "Whether tasks write their progress to a checkpoint, how often they do it, and how long writing one takes. Sweeping it measures what recovery costs.",
    exportModels:
        "How often the simulator writes a metrics snapshot. A shorter interval draws finer graphs and leaves more output to store.",
}

export function axisEntryLabels(axes: ExperimentAxes, key: AxisKey): string[] {
    switch (key) {
        case "topologies":
            return axes.topologies.map(topologyLabel)
        case "workloads":
            return axes.workloads.map(workloadLabel)
        case "allocationPolicies":
            return axes.allocationPolicies.map(schedulerLabel)
        case "failureModels":
            return axes.failureModels.map(failureLabel)
        case "maxNumFailures":
            return axes.maxNumFailures.map((value) => `${formatCount(value)} failures`)
        case "checkpointModels":
            return axes.checkpointModels.map(checkpointLabel)
        case "exportModels":
            return axes.exportModels.map(exportLabel)
    }
}

export function topologyLabel(topology: TopologySpec): string {
    const capacity = topologyCapacity(topology)
    const [only] = topology.datacenters
    const name =
        topology.datacenters.length === 1 && only ? dataCenterName(only) : `${topology.datacenters.length} data centers`
    return `${name} (${formatCount(capacity.hosts)} hosts)`
}

export function workloadLabel(workload: WorkloadSpec): string {
    if (workload.type === "inline") return `${formatCount(workload.tasks.length)} inline tasks`
    const fraction = workload.sampleFraction
    const name = referenceLabel(workload.source)
    return fraction === undefined || fraction >= 1 ? name : `${name} at ${Math.round(fraction * 100)}%`
}

export function schedulerLabel(policy: AllocationPolicySpec): string {
    if (policy.type === "prefab") return policy.prefabName ?? "default"
    return policy.type
}

export function failureLabel(model: FailureModelSpec): string {
    if (model.type === "none") return "No failures"
    if (model.type === "prefab") return model.prefabName
    if (model.type === "traceBased") return referenceLabel(model.source)
    return "Custom"
}

export function checkpointLabel(checkpoint: CheckpointSpec | null): string {
    if (checkpoint === null) return "No checkpoints"
    return checkpoint.interval === undefined ? "Default interval" : `Every ${formatWire("time", checkpoint.interval)}`
}

export function exportLabel(model: ExportSpec): string {
    return model.exportInterval === undefined ? "Default interval" : `Every ${formatWire("time", model.exportInterval)}`
}

function referenceLabel(reference: ResourceReference): string {
    return reference.type === "named" ? reference.name : reference.uri
}
