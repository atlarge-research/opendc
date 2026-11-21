import {
    type FloorCell,
    type FloorLayout,
    type FloorPlan,
    emptyFloor,
    freeCell,
    insertCell,
    placeOnFloor,
    reconcileFloor,
    reserveCell,
} from "@/lib/topology/layout"
import {
    type ClusterAddress,
    type ClusterSpec,
    type DataCenterSpec,
    type HostAddress,
    type HostSpec,
    type TopologySpec,
    dataCenterName,
} from "@/lib/topology/spec"

/**
 * A topology and its floor plan, edited together so that floors stay lined up with data centers
 * and cells with clusters. An edit aimed at something that does not exist returns the plan as it was.
 */
export interface TopologyPlan {
    topology: TopologySpec
    layout: FloorPlan
}

interface Located {
    dataCenter: DataCenterSpec
    floor: FloorLayout
}

export function addDataCenter(plan: TopologyPlan, dataCenter: DataCenterSpec): TopologyPlan {
    return {
        topology: { ...plan.topology, datacenters: [...plan.topology.datacenters, dataCenter] },
        layout: {
            ...plan.layout,
            floors: [...plan.layout.floors, reconcileFloor(emptyFloor(), dataCenter.clusters.length)],
        },
    }
}

export function updateDataCenter(plan: TopologyPlan, dataCenter: number, patch: Partial<DataCenterSpec>): TopologyPlan {
    return withDataCenter(plan, dataCenter, (located) => ({
        ...located,
        dataCenter: { ...located.dataCenter, ...patch },
    }))
}

/** Puts a copy right after its source, under a name of its own and with the same floor. */
export function duplicateDataCenter(plan: TopologyPlan, dataCenter: number): TopologyPlan {
    const source = plan.topology.datacenters[dataCenter]
    const floor = plan.layout.floors[dataCenter]
    if (!source || !floor) return plan
    const taken = plan.topology.datacenters.map(dataCenterName)
    const copy = { ...source, name: copyName(dataCenterName(source), taken) }
    return {
        topology: { ...plan.topology, datacenters: inserted(plan.topology.datacenters, dataCenter + 1, copy) },
        layout: { ...plan.layout, floors: inserted(plan.layout.floors, dataCenter + 1, floor) },
    }
}

export function removeDataCenter(plan: TopologyPlan, dataCenter: number): TopologyPlan {
    if (!plan.topology.datacenters[dataCenter]) return plan
    return {
        topology: { ...plan.topology, datacenters: plan.topology.datacenters.filter((_, at) => at !== dataCenter) },
        layout: { ...plan.layout, floors: plan.layout.floors.filter((_, at) => at !== dataCenter) },
    }
}

export function addCluster(
    plan: TopologyPlan,
    dataCenter: number,
    cluster: ClusterSpec,
    cell: FloorCell,
): TopologyPlan {
    return withDataCenter(plan, dataCenter, (located) => ({
        dataCenter: { ...located.dataCenter, clusters: [...located.dataCenter.clusters, cluster] },
        floor: insertCell(located.floor, located.dataCenter.clusters.length, cell),
    }))
}

export function removeClusters(plan: TopologyPlan, dataCenter: number, clusters: number[]): TopologyPlan {
    const gone = new Set(clusters)
    return withDataCenter(plan, dataCenter, (located) => ({
        dataCenter: { ...located.dataCenter, clusters: located.dataCenter.clusters.filter((_, at) => !gone.has(at)) },
        floor: { ...located.floor, cells: located.floor.cells.filter((_, at) => !gone.has(at)) },
    }))
}

/** Puts a copy right after its source, on the next free cell. */
export function duplicateCluster(plan: TopologyPlan, at: ClusterAddress): TopologyPlan {
    return withDataCenter(plan, at.dataCenter, (located) => {
        const cluster = located.dataCenter.clusters[at.cluster]
        const origin = located.floor.cells[at.cluster]
        if (!cluster || !origin) return located
        const reserved = reserveCell(located.floor, origin)
        return {
            dataCenter: {
                ...located.dataCenter,
                clusters: inserted(located.dataCenter.clusters, at.cluster + 1, cluster),
            },
            floor: insertCell(reserved.floor, at.cluster + 1, reserved.cell),
        }
    })
}

export function placeCluster(plan: TopologyPlan, at: ClusterAddress, cell: FloorCell): TopologyPlan {
    return withDataCenter(plan, at.dataCenter, (located) => ({
        ...located,
        floor: placeOnFloor(located.floor, at.cluster, cell),
    }))
}

export function updateClusters(
    plan: TopologyPlan,
    dataCenter: number,
    clusters: number[],
    patch: Partial<ClusterSpec>,
): TopologyPlan {
    const targets = new Set(clusters)
    return withDataCenter(plan, dataCenter, (located) => ({
        ...located,
        dataCenter: {
            ...located.dataCenter,
            clusters: located.dataCenter.clusters.map((cluster, at) =>
                targets.has(at) ? { ...cluster, ...patch } : cluster,
            ),
        },
    }))
}

/**
 * Moves clusters to another data center, where they are appended in the order they had and each
 * placed on the first free cell of its new floor.
 */
export function moveClusters(plan: TopologyPlan, from: number, clusters: number[], to: number): TopologyPlan {
    const source = plan.topology.datacenters[from]
    if (from === to || !source || !plan.topology.datacenters[to]) return plan
    const moving = [...new Set(clusters)]
        .sort((left, right) => left - right)
        .flatMap((index) => source.clusters[index] ?? [])
    if (moving.length === 0) return plan
    const removed = removeClusters(plan, from, clusters)
    return withDataCenter(removed, to, (located) =>
        moving.reduce((target, cluster) => {
            const claimed = freeCell(target.floor)
            return {
                dataCenter: { ...target.dataCenter, clusters: [...target.dataCenter.clusters, cluster] },
                floor: insertCell(claimed.floor, target.dataCenter.clusters.length, claimed.cell),
            }
        }, located),
    )
}

export function addHost(plan: TopologyPlan, at: ClusterAddress, host: HostSpec): TopologyPlan {
    return withHosts(plan, at, (hosts) => [...hosts, host])
}

export function removeHost(plan: TopologyPlan, at: HostAddress): TopologyPlan {
    return withHosts(plan, at, (hosts) => hosts.filter((_, index) => index !== at.host))
}

export function duplicateHost(plan: TopologyPlan, at: HostAddress): TopologyPlan {
    return withHosts(plan, at, (hosts) => {
        const source = hosts[at.host]
        return source ? inserted(hosts, at.host + 1, source) : hosts
    })
}

export function updateHost(plan: TopologyPlan, at: HostAddress, patch: Partial<HostSpec>): TopologyPlan {
    return withHosts(plan, at, (hosts) =>
        hosts.map((host, index) => (index === at.host ? { ...host, ...patch } : host)),
    )
}

function withHosts(plan: TopologyPlan, at: ClusterAddress, transform: (hosts: HostSpec[]) => HostSpec[]): TopologyPlan {
    return withDataCenter(plan, at.dataCenter, (located) => {
        const cluster = located.dataCenter.clusters[at.cluster]
        if (!cluster) return located
        const changed = { ...cluster, hosts: transform(cluster.hosts) }
        return {
            ...located,
            dataCenter: {
                ...located.dataCenter,
                clusters: located.dataCenter.clusters.map((existing, index) =>
                    index === at.cluster ? changed : existing,
                ),
            },
        }
    })
}

function withDataCenter(
    plan: TopologyPlan,
    dataCenter: number,
    transform: (located: Located) => Located,
): TopologyPlan {
    const target = plan.topology.datacenters[dataCenter]
    const floor = plan.layout.floors[dataCenter]
    if (!target || !floor) return plan
    const located = { dataCenter: target, floor }
    const changed = transform(located)
    if (changed === located) return plan
    return {
        topology: {
            ...plan.topology,
            datacenters: plan.topology.datacenters.map((existing, at) =>
                at === dataCenter ? changed.dataCenter : existing,
            ),
        },
        layout: {
            ...plan.layout,
            floors: plan.layout.floors.map((existing, at) => (at === dataCenter ? changed.floor : existing)),
        },
    }
}

function copyName(name: string, taken: string[]): string {
    const used = new Set(taken)
    let candidate = `${name} copy`
    for (let attempt = 2; used.has(candidate); attempt++) candidate = `${name} copy ${attempt}`
    return candidate
}

function inserted<T>(items: T[], index: number, item: T): T[] {
    const next = [...items]
    next.splice(index, 0, item)
    return next
}
