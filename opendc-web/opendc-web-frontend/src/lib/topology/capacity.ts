import {
    type ClusterSpec,
    type DataCenterSpec,
    type HostSpec,
    type TopologySpec,
    clusterCount,
    cpuCount,
    gpuCount,
    hostCount,
    peakPower,
} from "@/lib/topology/spec"
import { type Quantity, parseQuantity } from "@/lib/units"

export interface Capacity {
    dataCenters: number
    clusters: number
    hosts: number
    cores: number
    gpus: number
    memoryMiB: number
    peakPowerW: number
}

export type PowerBudget = { status: "unlimited" } | { status: "limited"; watts: number }

export interface PowerHeadroom {
    usedW: number
    budget: PowerBudget
}

/** What one cluster draws, and what part of its data center's supply that is. */
export type SupplyShare =
    | { status: "unlimited"; drawW: number }
    | { status: "limited"; drawW: number; fraction: number }

/**
 * The simulator's default supply is `Long.MAX_VALUE` W, which is written as
 * "9223372036854776.000000 KWatts" and reads back as exactly this.
 */
export const UNLIMITED_SUPPLY_W = 2 ** 63

export const WATTS_PER_KW = 1000

const EMPTY: Capacity = { dataCenters: 0, clusters: 0, hosts: 0, cores: 0, gpus: 0, memoryMiB: 0, peakPowerW: 0 }

export function topologyCapacity(topology: TopologySpec): Capacity {
    return topology.datacenters.map(dataCenterCapacity).reduce(addCapacity, EMPTY)
}

export function dataCenterCapacity(dataCenter: DataCenterSpec): Capacity {
    return { ...dataCenter.clusters.map(clusterCapacity).reduce(addCapacity, EMPTY), dataCenters: 1 }
}

export function clusterCapacity(cluster: ClusterSpec): Capacity {
    const instances = clusterCount(cluster)
    const perInstance = cluster.hosts.map(hostCapacity).reduce(addCapacity, EMPTY)
    return {
        dataCenters: 0,
        clusters: instances,
        hosts: perInstance.hosts * instances,
        cores: perInstance.cores * instances,
        gpus: perInstance.gpus * instances,
        memoryMiB: perInstance.memoryMiB * instances,
        peakPowerW: perInstance.peakPowerW * instances,
    }
}

export function hostCapacity(host: HostSpec): Capacity {
    const instances = hostCount(host)
    const gpus = host.gpu ? gpuCount(host.gpu) : 0
    const cpuPower = host.cpuPowerModel ? watts(peakPower(host.cpuPowerModel)) : 0
    const gpuPower = host.gpu && host.gpuPowerModel ? watts(peakPower(host.gpuPowerModel)) : 0
    return {
        dataCenters: 0,
        clusters: 0,
        hosts: instances,
        cores: host.cpu.coreCount * cpuCount(host.cpu) * instances,
        gpus: gpus * instances,
        memoryMiB: measure("dataSize", host.memory.size) * instances,
        peakPowerW: (cpuPower + gpuPower) * instances,
    }
}

/** Absent, unreadable, or the simulator's own unlimited default all mean no limit. */
export function supplyOf(dataCenter: DataCenterSpec): PowerBudget {
    const declared = dataCenter.powerSource?.maxPower
    if (declared === undefined) return { status: "unlimited" }
    const parsed = parseQuantity("power", declared)
    if (parsed.status !== "ok" || parsed.base >= UNLIMITED_SUPPLY_W) return { status: "unlimited" }
    return { status: "limited", watts: parsed.base }
}

export function dataCenterHeadroom(dataCenter: DataCenterSpec): PowerHeadroom {
    return { usedW: dataCenterCapacity(dataCenter).peakPowerW, budget: supplyOf(dataCenter) }
}

export function clusterShare(cluster: ClusterSpec, supply: PowerBudget): SupplyShare {
    const drawW = clusterCapacity(cluster).peakPowerW
    if (supply.status === "unlimited") return { status: "unlimited", drawW }
    const fraction = supply.watts > 0 ? drawW / supply.watts : drawW > 0 ? Number.POSITIVE_INFINITY : 0
    return { status: "limited", drawW, fraction }
}

export function isOverBudget(headroom: PowerHeadroom): boolean {
    return headroom.budget.status === "limited" && headroom.usedW > headroom.budget.watts
}

export function overBudgetDataCenters(topology: TopologySpec): number[] {
    return topology.datacenters.flatMap((dataCenter, index) =>
        isOverBudget(dataCenterHeadroom(dataCenter)) ? [index] : [],
    )
}

/** The peak draw rounded up to a whole kilowatt, and never less than one. */
export function suggestedSupplyW(dataCenter: DataCenterSpec): number {
    const peak = dataCenterCapacity(dataCenter).peakPowerW
    return Math.max(WATTS_PER_KW, Math.ceil(peak / WATTS_PER_KW) * WATTS_PER_KW)
}

function addCapacity(left: Capacity, right: Capacity): Capacity {
    return {
        dataCenters: left.dataCenters + right.dataCenters,
        clusters: left.clusters + right.clusters,
        hosts: left.hosts + right.hosts,
        cores: left.cores + right.cores,
        gpus: left.gpus + right.gpus,
        memoryMiB: left.memoryMiB + right.memoryMiB,
        peakPowerW: left.peakPowerW + right.peakPowerW,
    }
}

function watts(value: Quantity): number {
    return measure("power", value)
}

function measure(kind: "power" | "dataSize", value: Quantity): number {
    const parsed = parseQuantity(kind, value)
    return parsed.status === "ok" ? parsed.base : 0
}
