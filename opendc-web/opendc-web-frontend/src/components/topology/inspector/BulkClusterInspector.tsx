"use client"

import { ShortcutGuide } from "@/components/topology/ShortcutGuide"
import { DataCenterSelect } from "@/components/topology/inspector/DataCenterSelect"
import type { TopologyPlan } from "@/lib/topology/edits"
import { updateClusters } from "@/lib/topology/edits"
import { type DataCenterSpec, clusterName } from "@/lib/topology/spec"
import { Divider, NumberInput, ScrollArea, Stack, Text } from "@mantine/core"

/** What applies to several clusters of one data center at once. */
export function BulkClusterInspector({
    dataCenter,
    index,
    clusters,
    dataCenterNames,
    apply,
    onMove,
}: {
    dataCenter: DataCenterSpec
    index: number
    clusters: number[]
    dataCenterNames: string[]
    apply: (change: (plan: TopologyPlan) => TopologyPlan) => void
    onMove: (to: number) => void
}) {
    const names = clusters.flatMap((cluster) => {
        const spec = dataCenter.clusters[cluster]
        return spec ? [clusterName(spec)] : []
    })

    return (
        <ScrollArea h="100%" p="md">
            <Stack gap="sm">
                <Text fw={600}>{clusters.length} clusters selected</Text>
                <Text size="xs" c="dimmed" lineClamp={2}>
                    {names.join(", ")}
                </Text>
                <Divider label="Applies to all" />
                <NumberInput
                    label="Copies"
                    size="xs"
                    min={1}
                    placeholder="Set for every selected cluster"
                    onChange={(value) =>
                        typeof value === "number" &&
                        value >= 1 &&
                        apply((current) => updateClusters(current, index, clusters, { count: value }))
                    }
                />
                <DataCenterSelect
                    label="Move to data center"
                    names={dataCenterNames}
                    value={index}
                    onChange={(to) => to !== index && onMove(to)}
                />
                <Divider />
                <ShortcutGuide />
            </Stack>
        </ScrollArea>
    )
}
