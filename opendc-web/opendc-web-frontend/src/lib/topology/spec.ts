import type { Quantity } from "@/lib/units"
import { z } from "zod"

const quantity = z.union([z.number(), z.string()])

export const POWER_MODEL_TYPES = ["constant", "linear", "square", "cubic", "sqrt", "mse", "asymptotic"] as const

export const resourceReferenceSchema = z.discriminatedUnion("type", [
    z.object({ type: z.literal("named"), name: z.string() }).passthrough(),
    z.object({ type: z.literal("uri"), uri: z.string() }).passthrough(),
])

export const distributionPolicySchema = z.discriminatedUnion("type", [
    z.object({ type: z.literal("maxMinFairness") }).passthrough(),
    z.object({ type: z.literal("bestEffort"), updateInterval: quantity.optional() }).passthrough(),
    z.object({ type: z.literal("equalShare") }).passthrough(),
    z.object({ type: z.literal("firstFit") }).passthrough(),
    z.object({ type: z.literal("fixedShare"), shareRatio: z.number().optional() }).passthrough(),
])

export const virtualizationOverheadSchema = z.discriminatedUnion("type", [
    z.object({ type: z.literal("none") }).passthrough(),
    z.object({ type: z.literal("constant"), percentageOverhead: z.number().nullish() }).passthrough(),
    z.object({ type: z.literal("shareBased") }).passthrough(),
])

export const batteryPolicySchema = z.discriminatedUnion("type", [
    z.object({ type: z.literal("single"), carbonThreshold: z.number() }).passthrough(),
    z.object({ type: z.literal("double"), lowerThreshold: z.number(), upperThreshold: z.number() }).passthrough(),
    z.object({ type: z.literal("runningMean"), startingThreshold: z.number(), windowSize: z.number() }).passthrough(),
    z
        .object({ type: z.literal("runningMeanPlus"), startingThreshold: z.number(), windowSize: z.number() })
        .passthrough(),
])

const maxIdle = { maxPower: quantity, idlePower: quantity }

/** Each type carries only its own fields: the server refuses a key a type does not have. */
export const powerSpecSchema = z.discriminatedUnion("type", [
    z.object({ type: z.literal("constant"), power: quantity }).passthrough(),
    z.object({ type: z.literal("linear"), ...maxIdle }).passthrough(),
    z.object({ type: z.literal("square"), ...maxIdle }).passthrough(),
    z.object({ type: z.literal("cubic"), ...maxIdle }).passthrough(),
    z.object({ type: z.literal("sqrt"), ...maxIdle }).passthrough(),
    z.object({ type: z.literal("mse"), ...maxIdle, calibrationFactor: z.number() }).passthrough(),
    z
        .object({ type: z.literal("asymptotic"), ...maxIdle, asymUtil: z.number(), dvfs: z.boolean().optional() })
        .passthrough(),
])

export const carbonModelSchema = z.discriminatedUnion("type", [
    z.object({ type: z.literal("trace"), source: resourceReferenceSchema }).passthrough(),
    z
        .object({
            type: z.literal("inline-trace"),
            intensities: z.array(z.object({ timestamp: quantity, carbonIntensity: z.number() }).passthrough()),
        })
        .passthrough(),
])

export const cpuSpecSchema = z
    .object({
        coreCount: z.number(),
        coreSpeed: quantity,
        count: z.number().optional(),
        vendor: z.string().optional(),
        modelName: z.string().optional(),
        architecture: z.string().optional(),
    })
    .passthrough()

export const memorySpecSchema = z
    .object({
        size: quantity,
        speed: quantity.optional(),
        vendor: z.string().optional(),
        modelName: z.string().optional(),
        architecture: z.string().optional(),
    })
    .passthrough()

export const gpuSpecSchema = z
    .object({
        coreCount: z.number(),
        coreSpeed: quantity,
        count: z.number().optional(),
        memory: quantity.optional(),
        memoryBandwidth: quantity.optional(),
        vendor: z.string().optional(),
        modelName: z.string().optional(),
        architecture: z.string().optional(),
        virtualizationOverhead: virtualizationOverheadSchema.optional(),
    })
    .passthrough()

export const hostSpecSchema = z
    .object({
        name: z.string().optional(),
        count: z.number().optional(),
        cpu: cpuSpecSchema,
        memory: memorySpecSchema,
        gpu: gpuSpecSchema.nullish(),
        cpuPowerModel: powerSpecSchema.optional(),
        gpuPowerModel: powerSpecSchema.optional(),
        cpuDistribution: distributionPolicySchema.optional(),
        gpuDistribution: distributionPolicySchema.optional(),
    })
    .passthrough()

export const powerSourceSpecSchema = z
    .object({
        name: z.string().optional(),
        maxPower: quantity.optional(),
        carbon: carbonModelSchema.nullish(),
    })
    .passthrough()

export const batterySpecSchema = z
    .object({
        name: z.string().optional(),
        capacity: quantity,
        chargingSpeed: quantity,
        initialCharge: quantity.optional(),
        policy: batteryPolicySchema,
        embodiedCarbon: z.number().optional(),
        expectedLifetime: z.number().optional(),
    })
    .passthrough()

export const clusterSpecSchema = z
    .object({
        name: z.string().optional(),
        count: z.number().optional(),
        hosts: z.array(hostSpecSchema),
    })
    .passthrough()

/** Power, battery and carbon belong to the data center: its clusters share one supply. */
export const dataCenterSpecSchema = z
    .object({
        name: z.string().optional(),
        clusters: z.array(clusterSpecSchema),
        powerSource: powerSourceSpecSchema.optional(),
        battery: batterySpecSchema.nullish(),
    })
    .passthrough()

/**
 * Only the data-center form. The server converts a legacy `clusters` document once, with the
 * simulator's own conversion, so a copy of that conversion here could only drift from it.
 */
export const topologySpecSchema = z.object({ datacenters: z.array(dataCenterSpecSchema) }).passthrough()

export type ResourceReference = z.infer<typeof resourceReferenceSchema>
export type DistributionPolicy = z.infer<typeof distributionPolicySchema>
export type VirtualizationOverhead = z.infer<typeof virtualizationOverheadSchema>
export type BatteryPolicy = z.infer<typeof batteryPolicySchema>
export type PowerSpec = z.infer<typeof powerSpecSchema>
export type PowerModelType = PowerSpec["type"]
export type CarbonModel = z.infer<typeof carbonModelSchema>
export type CpuSpec = z.infer<typeof cpuSpecSchema>
export type MemorySpec = z.infer<typeof memorySpecSchema>
export type GpuSpec = z.infer<typeof gpuSpecSchema>
export type HostSpec = z.infer<typeof hostSpecSchema>
export type PowerSourceSpec = z.infer<typeof powerSourceSpecSchema>
export type BatterySpec = z.infer<typeof batterySpecSchema>
export type ClusterSpec = z.infer<typeof clusterSpecSchema>
export type DataCenterSpec = z.infer<typeof dataCenterSpecSchema>
export type TopologySpec = z.infer<typeof topologySpecSchema>

export interface ClusterAddress {
    dataCenter: number
    cluster: number
}

export interface HostAddress extends ClusterAddress {
    host: number
}

export const DEFAULT_DATA_CENTER_NAME = "DC"
export const DEFAULT_CLUSTER_NAME = "Cluster"
export const DEFAULT_HOST_NAME = "Host"
export const DEFAULT_COUNT = 1

export function clusterCount(cluster: ClusterSpec): number {
    return cluster.count ?? DEFAULT_COUNT
}

export function hostCount(host: HostSpec): number {
    return host.count ?? DEFAULT_COUNT
}

export function cpuCount(cpu: CpuSpec): number {
    return cpu.count ?? DEFAULT_COUNT
}

export function gpuCount(gpu: GpuSpec): number {
    return gpu.count ?? DEFAULT_COUNT
}

export function dataCenterName(dataCenter: DataCenterSpec): string {
    return dataCenter.name ?? DEFAULT_DATA_CENTER_NAME
}

export function clusterName(cluster: ClusterSpec): string {
    return cluster.name ?? DEFAULT_CLUSTER_NAME
}

export function hostName(host: HostSpec): string {
    return host.name ?? DEFAULT_HOST_NAME
}

export function isPowerModelType(type: string): type is PowerModelType {
    return (POWER_MODEL_TYPES as readonly string[]).includes(type)
}

/** What a component draws at full load, which a constant model draws at every load. */
export function peakPower(model: PowerSpec): Quantity {
    return model.type === "constant" ? model.power : model.maxPower
}

/** What a component draws doing nothing. */
export function idlePower(model: PowerSpec): Quantity {
    return model.type === "constant" ? model.power : model.idlePower
}

/** The carbon trace a power source names, if it reads one rather than carrying its own. */
export function carbonTraceName(carbon: CarbonModel | null | undefined): string | null {
    return carbon?.type === "trace" && carbon.source.type === "named" ? carbon.source.name : null
}
