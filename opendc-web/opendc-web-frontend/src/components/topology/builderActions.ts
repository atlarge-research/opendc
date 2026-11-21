import { newCluster, newDataCenter, nextName } from "@/components/topology/defaults"
import {
    type Selection,
    afterClusterRemoval,
    afterDataCenterRemoval,
    selectCluster,
    selectDataCenter,
    selectedClusters,
} from "@/components/topology/selection"
import type { TopologyEditor } from "@/components/topology/useTopologyEditor"
import {
    addCluster,
    addDataCenter,
    duplicateCluster,
    duplicateDataCenter,
    moveClusters,
    removeClusters,
    removeDataCenter,
} from "@/lib/topology/edits"
import type { FloorCell } from "@/lib/topology/layout"
import { clusterName, dataCenterName } from "@/lib/topology/spec"

/** The builder's edits that also move what is selected, acting on the floor on screen. */
export interface BuilderActions {
    createAt: (cell: FloorCell) => void
    duplicateSelected: () => void
    deleteSelected: () => void
    addDataCenter: () => void
    duplicateDataCenter: (dataCenter: number) => void
    removeDataCenter: (dataCenter: number) => void
    moveClusters: (from: number, clusters: number[], to: number) => void
}

export function builderActions(editor: TopologyEditor): BuilderActions {
    const { plan, view } = editor
    const floor = view.floor
    const dataCenters = plan.topology.datacenters

    return {
        createAt: (cell) => {
            const dataCenter = dataCenters[floor]
            if (!dataCenter) return
            const cluster = newCluster(nextName("Cluster", dataCenter.clusters.map(clusterName)))
            editor.apply((current) => addCluster(current, floor, cluster, cell))
            editor.select(selectCluster({ dataCenter: floor, cluster: dataCenter.clusters.length }))
        },
        duplicateSelected: () => {
            const [first] = selectedClusters(view.selection, floor)
            if (first === undefined) return
            editor.apply((current) => duplicateCluster(current, { dataCenter: floor, cluster: first }))
            editor.select(selectCluster({ dataCenter: floor, cluster: first + 1 }))
        },
        deleteSelected: () => {
            const chosen = selectedClusters(view.selection, floor)
            if (chosen.length === 0) return
            editor.apply((current) => removeClusters(current, floor, chosen))
            editor.show(afterClusterRemoval(view, floor, chosen))
        },
        addDataCenter: () => {
            const added = newDataCenter(nextName("DC", dataCenters.map(dataCenterName)))
            editor.apply((current) => addDataCenter(current, added))
            editor.select(selectDataCenter(dataCenters.length))
        },
        duplicateDataCenter: (dataCenter) => {
            editor.apply((current) => duplicateDataCenter(current, dataCenter))
            editor.select(selectDataCenter(dataCenter + 1))
        },
        removeDataCenter: (dataCenter) => {
            if (dataCenters.length <= 1) return
            editor.apply((current) => removeDataCenter(current, dataCenter))
            editor.show(afterDataCenterRemoval(view, dataCenter))
        },
        moveClusters: (from, clusters, to) => {
            const source = dataCenters[from]
            const target = dataCenters[to]
            if (!source || !target || from === to) return
            const moved = [...new Set(clusters)].filter((index) => source.clusters[index] !== undefined).length
            editor.apply((current) => moveClusters(current, from, clusters, to))
            editor.select(appended(to, target.clusters.length, moved))
        },
    }
}

function appended(dataCenter: number, start: number, count: number): Selection {
    if (count === 0) return selectDataCenter(dataCenter)
    return { kind: "clusters", dataCenter, clusters: Array.from({ length: count }, (_, offset) => start + offset) }
}
