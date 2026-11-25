"use client"

import { BulkClusterInspector } from "@/components/topology/inspector/BulkClusterInspector"
import { ClusterInspector } from "@/components/topology/inspector/ClusterInspector"
import { DataCenterInspector } from "@/components/topology/inspector/DataCenterInspector"
import { TopologyOverview } from "@/components/topology/inspector/TopologyOverview"
import type { Selection } from "@/components/topology/selection"
import type { DocumentIssue } from "@/lib/api/types"
import type { TopologyPlan } from "@/lib/topology/edits"
import { updateDataCenter } from "@/lib/topology/edits"
import { type HostAddress, dataCenterName } from "@/lib/topology/spec"

export interface InspectorProps {
    plan: TopologyPlan
    selection: Selection
    issues: DocumentIssue[]
    apply: (change: (plan: TopologyPlan) => TopologyPlan) => void
    onSelectHost: (at: HostAddress) => void
    onMoveClusters: (from: number, clusters: number[], to: number) => void
    onDuplicateDataCenter: (dataCenter: number) => void
    onRemoveDataCenter: (dataCenter: number) => void
}

export function TopologyInspector(props: InspectorProps) {
    const { plan, selection, issues, apply } = props
    const overview = <TopologyOverview topology={plan.topology} issues={issues} />
    if (selection.kind === "topology") return overview

    const dataCenter = plan.topology.datacenters[selection.dataCenter]
    if (!dataCenter) return overview
    const names = plan.topology.datacenters.map(dataCenterName)

    switch (selection.kind) {
        case "dataCenter":
            return (
                <DataCenterInspector
                    dataCenter={dataCenter}
                    index={selection.dataCenter}
                    removable={plan.topology.datacenters.length > 1}
                    issues={issues}
                    onChange={(patch) => apply((current) => updateDataCenter(current, selection.dataCenter, patch))}
                    onDuplicate={() => props.onDuplicateDataCenter(selection.dataCenter)}
                    onRemove={() => props.onRemoveDataCenter(selection.dataCenter)}
                />
            )
        case "clusters": {
            const [first] = selection.clusters
            const cluster = first === undefined ? undefined : dataCenter.clusters[first]
            if (selection.clusters.length > 1 || first === undefined || !cluster) {
                return (
                    <BulkClusterInspector
                        dataCenter={dataCenter}
                        index={selection.dataCenter}
                        clusters={selection.clusters}
                        dataCenterNames={names}
                        apply={apply}
                        onMove={(to) => props.onMoveClusters(selection.dataCenter, selection.clusters, to)}
                    />
                )
            }
            const at = { dataCenter: selection.dataCenter, cluster: first }
            return (
                <ClusterInspector
                    cluster={cluster}
                    at={at}
                    selectedHost={-1}
                    dataCenterNames={names}
                    issues={issues}
                    onSelectHost={(host) => props.onSelectHost({ ...at, host })}
                    onMove={(to) => props.onMoveClusters(at.dataCenter, [at.cluster], to)}
                    apply={apply}
                />
            )
        }
        case "host": {
            const cluster = dataCenter.clusters[selection.cluster]
            if (!cluster) return overview
            const at = { dataCenter: selection.dataCenter, cluster: selection.cluster }
            return (
                <ClusterInspector
                    cluster={cluster}
                    at={at}
                    selectedHost={selection.host}
                    dataCenterNames={names}
                    issues={issues}
                    onSelectHost={(host) => props.onSelectHost({ ...at, host })}
                    onMove={(to) => props.onMoveClusters(at.dataCenter, [at.cluster], to)}
                    apply={apply}
                />
            )
        }
    }
}
