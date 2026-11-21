import {
    type Selection,
    WHOLE_TOPOLOGY,
    selectCluster,
    selectDataCenter,
    selectedClusters,
    toggleHost,
} from "@/components/topology/selection"
import {
    type ClusterAddress,
    type HostAddress,
    type TopologySpec,
    clusterCount,
    clusterName,
    dataCenterName,
    hostCount,
    hostName,
} from "@/lib/topology/spec"
import { clusterPath, dataCenterPath, hostPath } from "@/lib/topology/validation"
import type { TreeNodeData } from "@mantine/core"

export type TreeTarget =
    | { kind: "topology" }
    | { kind: "dataCenter"; dataCenter: number }
    | { kind: "cluster"; at: ClusterAddress }
    | { kind: "host"; at: HostAddress }

/** The tree's nodes, and what each one stands for, looked up by value rather than parsed out of it. */
export interface TopologyTreeModel {
    data: TreeNodeData[]
    targets: Map<string, TreeTarget>
}

export const ROOT_VALUE = "topology"

export function buildTree(topology: TopologySpec): TopologyTreeModel {
    const targets = new Map<string, TreeTarget>([[ROOT_VALUE, { kind: "topology" }]])
    const dataCenters = topology.datacenters.map((dataCenter, d) => {
        const value = `dc-${d}`
        targets.set(value, { kind: "dataCenter", dataCenter: d })
        return {
            value,
            label: dataCenterName(dataCenter),
            children: dataCenter.clusters.map((cluster, c) => {
                const clusterValue = `${value}/c-${c}`
                targets.set(clusterValue, { kind: "cluster", at: { dataCenter: d, cluster: c } })
                return {
                    value: clusterValue,
                    label: `${clusterName(cluster)}${clusterCount(cluster) > 1 ? ` x${clusterCount(cluster)}` : ""}`,
                    children: cluster.hosts.map((host, h) => {
                        const hostValue = `${clusterValue}/h-${h}`
                        targets.set(hostValue, { kind: "host", at: { dataCenter: d, cluster: c, host: h } })
                        return { value: hostValue, label: `${hostName(host)} x${hostCount(host)}` }
                    }),
                }
            }),
        }
    })
    return { data: [{ value: ROOT_VALUE, label: "Topology", children: dataCenters }], targets }
}

/** Where a target's issues are reported. The whole topology owns every issue. */
export function targetPath(target: TreeTarget): string {
    switch (target.kind) {
        case "topology":
            return ""
        case "dataCenter":
            return dataCenterPath(target.dataCenter)
        case "cluster":
            return clusterPath(target.at)
        case "host":
            return hostPath(target.at)
    }
}

export function isActive(target: TreeTarget, selection: Selection): boolean {
    switch (target.kind) {
        case "topology":
            return selection.kind === "topology"
        case "dataCenter":
            return selection.kind === "dataCenter" && selection.dataCenter === target.dataCenter
        case "cluster":
            return (
                selection.kind === "clusters" &&
                selectedClusters(selection, target.at.dataCenter).includes(target.at.cluster)
            )
        case "host":
            return (
                selection.kind === "host" &&
                selection.dataCenter === target.at.dataCenter &&
                selection.cluster === target.at.cluster &&
                selection.host === target.at.host
            )
    }
}

/** What clicking a node selects. A host closes again when it is the one already open. */
export function selectionFor(target: TreeTarget, selection: Selection): Selection {
    switch (target.kind) {
        case "topology":
            return WHOLE_TOPOLOGY
        case "dataCenter":
            return selectDataCenter(target.dataCenter)
        case "cluster":
            return selectCluster(target.at)
        case "host":
            return toggleHost(selection, target.at)
    }
}
