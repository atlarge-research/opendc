import type { DocumentIssue } from "@/lib/api/types"
import type {
    ClusterAddress,
    ClusterSpec,
    CpuSpec,
    DataCenterSpec,
    HostAddress,
    HostSpec,
    PowerSpec,
    TopologySpec,
} from "@/lib/topology/spec"
import { hostCount } from "@/lib/topology/spec"
import { type Quantity, type QuantityKind, parseQuantity } from "@/lib/units"

export function dataCenterPath(dataCenter: number): string {
    return `datacenters[${dataCenter}]`
}

export function clusterPath(at: ClusterAddress): string {
    return `${dataCenterPath(at.dataCenter)}.clusters[${at.cluster}]`
}

export function hostPath(at: HostAddress): string {
    return `${clusterPath(at)}.hosts[${at.host}]`
}

/** The same paths the simulator's own validation reports, so the server's issues land in the same places. */
export function validateTopology(topology: TopologySpec): DocumentIssue[] {
    const issues: DocumentIssue[] = []
    if (topology.datacenters.length === 0) issues.push({ path: "datacenters", message: "must not be empty" })
    topology.datacenters.forEach((dataCenter, index) => issues.push(...validateDataCenter(dataCenter, index)))
    return issues
}

/** The data centers with an issue anywhere inside them. */
export function brokenDataCenters(issues: DocumentIssue[], count: number): number[] {
    return indices(count).filter((index) => issuesUnder(issues, dataCenterPath(index)).length > 0)
}

/** The clusters of one data center with an issue anywhere inside them. */
export function brokenClusters(issues: DocumentIssue[], dataCenter: number, count: number): number[] {
    return indices(count).filter((cluster) => issuesUnder(issues, clusterPath({ dataCenter, cluster })).length > 0)
}

function validateDataCenter(dataCenter: DataCenterSpec, index: number): DocumentIssue[] {
    const path = dataCenterPath(index)
    const issues: DocumentIssue[] = []
    if (dataCenter.clusters.length === 0) issues.push({ path: `${path}.clusters`, message: "must not be empty" })
    dataCenter.clusters.forEach((cluster, at) =>
        issues.push(...validateCluster(cluster, clusterPath({ dataCenter: index, cluster: at }))),
    )
    if (dataCenter.powerSource?.maxPower !== undefined) {
        issues.push(...measurement("power", dataCenter.powerSource.maxPower, `${path}.powerSource.maxPower`))
    }
    return issues
}

function validateCluster(cluster: ClusterSpec, path: string): DocumentIssue[] {
    const issues: DocumentIssue[] = []
    if (cluster.hosts.length === 0) issues.push({ path: `${path}.hosts`, message: "must not be empty" })
    cluster.hosts.forEach((host, index) => issues.push(...validateHost(host, `${path}.hosts[${index}]`)))
    return issues
}

function validateHost(host: HostSpec, path: string): DocumentIssue[] {
    const issues: DocumentIssue[] = []
    if (hostCount(host) <= 0) issues.push({ path: `${path}.count`, message: "must be > 0" })
    issues.push(...validateCpu(host.cpu, `${path}.cpu`))
    issues.push(...measurement("dataSize", host.memory.size, `${path}.memory.size`))
    if (host.cpuPowerModel) issues.push(...validatePowerSpec(host.cpuPowerModel, `${path}.cpuPowerModel`))
    if (host.gpu && host.gpuPowerModel) {
        issues.push(...validatePowerSpec(host.gpuPowerModel, `${path}.gpuPowerModel`))
    }
    return issues
}

function validateCpu(cpu: CpuSpec, path: string): DocumentIssue[] {
    const issues: DocumentIssue[] = []
    if (cpu.coreCount <= 0) issues.push({ path: `${path}.coreCount`, message: "must be > 0" })
    if ((cpu.count ?? 1) <= 0) issues.push({ path: `${path}.count`, message: "must be > 0" })
    issues.push(...measurement("frequency", cpu.coreSpeed, `${path}.coreSpeed`))
    return issues
}

function validatePowerSpec(power: PowerSpec, path: string): DocumentIssue[] {
    switch (power.type) {
        case "constant":
            return measurement("power", power.power, `${path}.power`)
        case "linear":
        case "square":
        case "cubic":
        case "sqrt":
            return validateMaxIdle(power, path)
        case "mse":
            return [...validateMaxIdle(power, path), ...positive(power.calibrationFactor, `${path}.calibrationFactor`)]
        case "asymptotic":
            return [...validateMaxIdle(power, path), ...positive(power.asymUtil, `${path}.asymUtil`)]
    }
}

function validateMaxIdle(power: { maxPower: Quantity; idlePower: Quantity }, path: string): DocumentIssue[] {
    const issues: DocumentIssue[] = []
    const max = parseQuantity("power", power.maxPower)
    const idle = parseQuantity("power", power.idlePower)
    issues.push(...measurement("power", power.maxPower, `${path}.maxPower`))
    issues.push(...measurement("power", power.idlePower, `${path}.idlePower`))
    if (max.status === "ok" && idle.status === "ok" && max.base < idle.base) {
        issues.push({ path: `${path}.maxPower`, message: "must be >= idlePower" })
    }
    return issues
}

function positive(value: number, path: string): DocumentIssue[] {
    return value > 0 ? [] : [{ path, message: "must be > 0" }]
}

const QUANTITY_LABEL: Record<QuantityKind, string> = {
    frequency: "frequency",
    dataSize: "data size",
    dataRate: "data rate",
    power: "power",
    energy: "energy",
    time: "duration",
}

function measurement(kind: QuantityKind, value: Quantity, path: string): DocumentIssue[] {
    const parsed = parseQuantity(kind, value)
    if (parsed.status === "invalid") return [{ path, message: `must be a valid ${QUANTITY_LABEL[kind]}` }]
    if (parsed.status === "unspecified") return [{ path, message: "must not be negative" }]
    return []
}

export function issuesUnder(issues: DocumentIssue[], prefix: string): DocumentIssue[] {
    return issues.filter((issue) => issue.path === prefix || issue.path.startsWith(`${prefix}.`))
}

function indices(count: number): number[] {
    return Array.from({ length: count }, (_, index) => index)
}
