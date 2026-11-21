import type { ClusterAddress, HostAddress, TopologySpec } from "@/lib/topology/spec"

export type Selection =
    | { kind: "topology" }
    | { kind: "dataCenter"; dataCenter: number }
    | { kind: "clusters"; dataCenter: number; clusters: number[] }
    | { kind: "host"; dataCenter: number; cluster: number; host: number }

/** What the builder shows: one data center's floor, and a selection the floor follows. */
export interface BuilderView {
    floor: number
    selection: Selection
}

export const WHOLE_TOPOLOGY: Selection = { kind: "topology" }

export const INITIAL_VIEW: BuilderView = { floor: 0, selection: WHOLE_TOPOLOGY }

export function selectDataCenter(dataCenter: number): Selection {
    return { kind: "dataCenter", dataCenter }
}

export function selectCluster(at: ClusterAddress): Selection {
    return { kind: "clusters", dataCenter: at.dataCenter, clusters: [at.cluster] }
}

export function selectHost(at: HostAddress): Selection {
    return { kind: "host", dataCenter: at.dataCenter, cluster: at.cluster, host: at.host }
}

// Closing the open group again is the only way out of it short of Escape, which drops the cluster.
export function toggleHost(selection: Selection, at: HostAddress): Selection {
    const open =
        selection.kind === "host" &&
        selection.dataCenter === at.dataCenter &&
        selection.cluster === at.cluster &&
        selection.host === at.host
    return open ? selectCluster(at) : selectHost(at)
}

/** The selected clusters of one data center, in index order. */
export function selectedClusters(selection: Selection, dataCenter: number): number[] {
    switch (selection.kind) {
        case "topology":
        case "dataCenter":
            return []
        case "clusters":
            return selection.dataCenter === dataCenter ? selection.clusters : []
        case "host":
            return selection.dataCenter === dataCenter ? [selection.cluster] : []
    }
}

/** Adds or removes a cluster. One in another data center starts a new selection there. */
export function extendToCluster(selection: Selection, at: ClusterAddress): Selection {
    const current = selectedClusters(selection, at.dataCenter)
    if (current.length === 0) return selectCluster(at)
    const next = current.includes(at.cluster)
        ? current.filter((entry) => entry !== at.cluster)
        : [...current, at.cluster]
    return withClusters(at.dataCenter, next)
}

/** The floor follows the selection; selecting the whole topology keeps the floor on screen. */
export function focus(view: BuilderView, selection: Selection): BuilderView {
    switch (selection.kind) {
        case "topology":
            return { floor: view.floor, selection }
        case "dataCenter":
        case "clusters":
        case "host":
            return { floor: selection.dataCenter, selection }
    }
}

/** Shifts the surviving clusters down, and falls back to their data center when none survive. */
export function afterClusterRemoval(view: BuilderView, dataCenter: number, removed: number[]): BuilderView {
    const { selection } = view
    if (selection.kind !== "clusters" && selection.kind !== "host") return view
    if (selection.dataCenter !== dataCenter) return view
    const gone = new Set(removed)
    if (selection.kind === "host") {
        if (gone.has(selection.cluster)) return { ...view, selection: selectDataCenter(dataCenter) }
        return { ...view, selection: { ...selection, cluster: shift(selection.cluster, removed) } }
    }
    const kept = selection.clusters.filter((index) => !gone.has(index)).map((index) => shift(index, removed))
    return { ...view, selection: withClusters(dataCenter, kept) }
}

export function afterDataCenterRemoval(view: BuilderView, dataCenter: number): BuilderView {
    const floor = view.floor > dataCenter || (view.floor === dataCenter && dataCenter > 0) ? view.floor - 1 : view.floor
    const { selection } = view
    if (selection.kind === "topology") return { floor, selection }
    if (selection.dataCenter === dataCenter) return { floor, selection: WHOLE_TOPOLOGY }
    const moved = selection.dataCenter > dataCenter ? selection.dataCenter - 1 : selection.dataCenter
    return { floor, selection: { ...selection, dataCenter: moved } }
}

/** Drops whatever an undo, redo or import left pointing past the end of the topology. */
export function clampToTopology(view: BuilderView, topology: TopologySpec): BuilderView {
    const floor = Math.max(0, Math.min(view.floor, topology.datacenters.length - 1))
    const { selection } = view
    if (selection.kind === "topology") return { floor, selection }
    const dataCenter = topology.datacenters[selection.dataCenter]
    if (!dataCenter) return { floor, selection: WHOLE_TOPOLOGY }
    switch (selection.kind) {
        case "dataCenter":
            return { floor, selection }
        case "clusters": {
            const kept = selection.clusters.filter((index) => index < dataCenter.clusters.length)
            return { floor, selection: withClusters(selection.dataCenter, kept) }
        }
        case "host": {
            const cluster = dataCenter.clusters[selection.cluster]
            if (!cluster) return { floor, selection: selectDataCenter(selection.dataCenter) }
            return { floor, selection: cluster.hosts[selection.host] ? selection : selectCluster(selection) }
        }
    }
}

function withClusters(dataCenter: number, clusters: number[]): Selection {
    if (clusters.length === 0) return selectDataCenter(dataCenter)
    return { kind: "clusters", dataCenter, clusters: [...clusters].sort((left, right) => left - right) }
}

function shift(index: number, removed: number[]): number {
    return index - removed.filter((entry) => entry < index).length
}
