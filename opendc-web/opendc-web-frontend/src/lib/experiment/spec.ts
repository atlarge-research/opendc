import { type TopologySpec, resourceReferenceSchema, topologySpecSchema } from "@/lib/topology/spec"
import { z } from "zod"

const quantity = z.union([z.number(), z.string()])

export const workloadSpecSchema = z.discriminatedUnion("type", [
    z
        .object({
            type: z.literal("trace"),
            source: resourceReferenceSchema,
            sampleFraction: z.number().optional(),
            submissionTime: z.string().nullish(),
            scalingPolicy: z.string().optional(),
            deferAll: z.boolean().optional(),
        })
        .passthrough(),
    z
        .object({
            type: z.literal("inline"),
            tasks: z.array(z.object({}).passthrough()),
            scalingPolicy: z.string().optional(),
        })
        .passthrough(),
])

export const allocationPolicySpecSchema = z.discriminatedUnion("type", [
    z.object({ type: z.literal("prefab"), prefabName: z.string().optional() }).passthrough(),
    z
        .object({
            type: z.literal("filter"),
            filters: z.array(z.unknown()).optional(),
            weighers: z.array(z.unknown()).optional(),
            subsetSize: z.number().optional(),
        })
        .passthrough(),
    z
        .object({
            type: z.literal("timeshift"),
            filters: z.array(z.unknown()).optional(),
            weighers: z.array(z.unknown()).optional(),
            windowSize: z.number().optional(),
        })
        .passthrough(),
])

export const failureModelSpecSchema = z.discriminatedUnion("type", [
    z.object({ type: z.literal("none") }).passthrough(),
    z
        .object({
            type: z.literal("traceBased"),
            source: resourceReferenceSchema,
            startPoint: z.number().optional(),
            repeat: z.boolean().optional(),
        })
        .passthrough(),
    z.object({ type: z.literal("prefab"), prefabName: z.string() }).passthrough(),
    z
        .object({
            type: z.literal("custom"),
            interArrival: z.unknown(),
            duration: z.unknown(),
            hostFraction: z.unknown(),
        })
        .passthrough(),
])

export const checkpointSpecSchema = z
    .object({
        interval: quantity.optional(),
        duration: quantity.optional(),
        intervalScaling: z.number().optional(),
    })
    .passthrough()

export const exportSpecSchema = z
    .object({
        exportInterval: quantity.optional(),
        printFrequency: z.number().nullish(),
        columns: z.object({}).passthrough().optional(),
        filesToExport: z.array(z.string()).optional(),
    })
    .passthrough()

export const experimentSpecSchema = z
    .object({
        name: z.string().optional(),
        topologies: z.array(topologySpecSchema),
        workloads: z.array(workloadSpecSchema),
        allocationPolicies: z.array(allocationPolicySpecSchema).optional(),
        failureModels: z.array(failureModelSpecSchema).optional(),
        maxNumFailures: z.array(z.number()).optional(),
        checkpointModels: z.array(checkpointSpecSchema.nullable()).optional(),
        exportModels: z.array(exportSpecSchema).optional(),
        runs: z.number().optional(),
        initialSeed: z.number().optional(),
    })
    .passthrough()

export type WorkloadSpec = z.infer<typeof workloadSpecSchema>
export type AllocationPolicySpec = z.infer<typeof allocationPolicySpecSchema>
export type FailureModelSpec = z.infer<typeof failureModelSpecSchema>
export type CheckpointSpec = z.infer<typeof checkpointSpecSchema>
export type ExportSpec = z.infer<typeof exportSpecSchema>
export type ExperimentSpec = z.infer<typeof experimentSpecSchema>

// The sdk-model's defaults, shown for omitted fields. Not ISO-8601: the editor writes these back.
export const DEFAULT_CHECKPOINT_INTERVAL = "1 h"
export const DEFAULT_CHECKPOINT_DURATION = "5 min"
export const DEFAULT_INTERVAL_SCALING = 1
export const DEFAULT_EXPORT_INTERVAL = "5 min"

export const DEFAULT_SCHEDULER = "Mem"
export const DEFAULT_ALLOCATION_POLICY: AllocationPolicySpec = { type: "prefab", prefabName: DEFAULT_SCHEDULER }
export const DEFAULT_FAILURE_MODEL: FailureModelSpec = { type: "none" }
export const DEFAULT_EXPORT_MODEL: ExportSpec = {}
export const DEFAULT_MAX_NUM_FAILURES = 10
export const DEFAULT_RUNS = 1

export function allocationPolicyAxis(spec: ExperimentSpec): AllocationPolicySpec[] {
    return spec.allocationPolicies ?? [DEFAULT_ALLOCATION_POLICY]
}

export function failureModelAxis(spec: ExperimentSpec): FailureModelSpec[] {
    return spec.failureModels ?? [DEFAULT_FAILURE_MODEL]
}

export function maxNumFailuresAxis(spec: ExperimentSpec): number[] {
    return spec.maxNumFailures ?? [DEFAULT_MAX_NUM_FAILURES]
}

export function checkpointModelAxis(spec: ExperimentSpec): Array<CheckpointSpec | null> {
    return spec.checkpointModels ?? [null]
}

export function exportModelAxis(spec: ExperimentSpec): ExportSpec[] {
    return spec.exportModels ?? [DEFAULT_EXPORT_MODEL]
}

export function experimentRuns(spec: ExperimentSpec): number {
    return spec.runs ?? DEFAULT_RUNS
}

// Axis entries are identified by position, so two that print the same label are still two entries.
export function positions(count: number): number[] {
    return Array.from({ length: count }, (_, position) => position)
}

export function scenarioCount(spec: ExperimentSpec): number {
    const axes = experimentAxes(spec)
    return AXIS_ORDER.reduce((total, key) => total * axes[key].length, 1)
}

export interface ExperimentAxes {
    maxNumFailures: number[]
    checkpointModels: Array<CheckpointSpec | null>
    failureModels: FailureModelSpec[]
    exportModels: ExportSpec[]
    allocationPolicies: AllocationPolicySpec[]
    workloads: WorkloadSpec[]
    topologies: TopologySpec[]
}

export type AxisKey = keyof ExperimentAxes

// Ordered from the fastest varying axis to the slowest, which is the order the backend flattens
// scenarios in. The index that ordering produces is the work shard identity, so it is contractual.
export const AXIS_ORDER: AxisKey[] = [
    "maxNumFailures",
    "checkpointModels",
    "failureModels",
    "exportModels",
    "allocationPolicies",
    "workloads",
    "topologies",
]

export function experimentAxes(spec: ExperimentSpec): ExperimentAxes {
    return {
        maxNumFailures: maxNumFailuresAxis(spec),
        checkpointModels: checkpointModelAxis(spec),
        failureModels: failureModelAxis(spec),
        exportModels: exportModelAxis(spec),
        allocationPolicies: allocationPolicyAxis(spec),
        workloads: spec.workloads,
        topologies: spec.topologies,
    }
}

export function scenarioCoordinates(axes: ExperimentAxes, scenarioIndex: number): Record<AxisKey, number> {
    const coordinates: Record<AxisKey, number> = {
        maxNumFailures: 0,
        checkpointModels: 0,
        failureModels: 0,
        exportModels: 0,
        allocationPolicies: 0,
        workloads: 0,
        topologies: 0,
    }
    let cursor = scenarioIndex

    for (const key of AXIS_ORDER) {
        const length = axes[key].length
        if (length === 0) break
        coordinates[key] = cursor % length
        cursor = Math.floor(cursor / length)
    }

    return coordinates
}
