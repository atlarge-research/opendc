"use client"

import { DataCenterSelect } from "@/components/topology/inspector/DataCenterSelect"
import { HostGroupList } from "@/components/topology/inspector/HostGroupList"
import { HostInspector } from "@/components/topology/inspector/HostInspector"
import { IssueList } from "@/components/topology/inspector/IssueList"
import { FieldLabel } from "@/components/util/FieldLabel"
import type { DocumentIssue } from "@/lib/api/types"
import { clusterCapacity } from "@/lib/topology/capacity"
import type { TopologyPlan } from "@/lib/topology/edits"
import { addHost, duplicateHost, removeHost, updateClusters, updateHost } from "@/lib/topology/edits"
import { type ClusterAddress, type ClusterSpec, type HostSpec, clusterCount, clusterName } from "@/lib/topology/spec"
import { clusterPath, issuesUnder } from "@/lib/topology/validation"
import { Badge, Box, Group, NumberInput, ScrollArea, Stack, Tabs, Text, TextInput } from "@mantine/core"
import { useEffect, useState } from "react"

export function ClusterInspector({
    cluster,
    at,
    selectedHost,
    dataCenterNames,
    issues,
    onSelectHost,
    onMove,
    apply,
}: {
    cluster: ClusterSpec
    at: ClusterAddress
    selectedHost: number
    dataCenterNames: string[]
    issues: DocumentIssue[]
    onSelectHost: (host: number) => void
    onMove: (dataCenter: number) => void
    apply: (change: (plan: TopologyPlan) => TopologyPlan) => void
}) {
    const [tab, setTab] = useState(selectedHost >= 0 ? "hosts" : "cluster")
    const path = clusterPath(at)
    const capacity = clusterCapacity(cluster)
    const host = cluster.hosts[selectedHost]
    const patch = (change: Partial<ClusterSpec>) =>
        apply((current) => updateClusters(current, at.dataCenter, [at.cluster], change))

    // Opening a host group, from the floor or the tree, is asking to see it.
    useEffect(() => {
        if (selectedHost >= 0) setTab("hosts")
    }, [selectedHost])

    return (
        <Stack h="100%" gap={0}>
            <Box px="md" pt="md" pb="xs">
                <Group justify="space-between" wrap="nowrap">
                    <Text fw={600} lineClamp={1}>
                        {clusterName(cluster)}
                    </Text>
                    <Badge variant="light" color="gray" size="sm">
                        {capacity.hosts} hosts
                    </Badge>
                </Group>
            </Box>

            <Tabs
                value={tab}
                onChange={(next) => next && setTab(next)}
                style={{ display: "flex", flexDirection: "column", minHeight: 0, flex: 1 }}
            >
                <Tabs.List px="sm">
                    <Tabs.Tab value="cluster">Cluster</Tabs.Tab>
                    <Tabs.Tab value="hosts">Hosts ({cluster.hosts.length})</Tabs.Tab>
                </Tabs.List>

                <ScrollArea style={{ flex: 1, minHeight: 0 }}>
                    <Box px="sm" pt="sm">
                        <IssueList issues={issuesUnder(issues, path)} prefix={`${path}.`} />
                    </Box>

                    <Tabs.Panel value="cluster" p="md">
                        <Stack gap="sm">
                            <TextInput
                                label="Name"
                                size="xs"
                                value={clusterName(cluster)}
                                onChange={(event) => patch({ name: event.currentTarget.value })}
                            />
                            <NumberInput
                                label={
                                    <FieldLabel
                                        label="Copies"
                                        help="Simulates this whole cluster this many times, hosts included."
                                    />
                                }
                                size="xs"
                                min={1}
                                value={clusterCount(cluster)}
                                onChange={(value) => patch({ count: typeof value === "number" ? value : 1 })}
                            />
                            <DataCenterSelect
                                label="Data center"
                                names={dataCenterNames}
                                value={at.dataCenter}
                                onChange={(to) => to !== at.dataCenter && onMove(to)}
                            />
                        </Stack>
                    </Tabs.Panel>

                    <Tabs.Panel value="hosts" p="md">
                        <Stack gap="sm">
                            <HostGroupList
                                cluster={cluster}
                                selectedHost={selectedHost}
                                onSelectHost={onSelectHost}
                                onAddHost={(added: HostSpec) => apply((current) => addHost(current, at, added))}
                                onRemoveHost={(chosen) =>
                                    apply((current) => removeHost(current, { ...at, host: chosen }))
                                }
                                onDuplicateHost={(chosen) =>
                                    apply((current) => duplicateHost(current, { ...at, host: chosen }))
                                }
                                onCountChange={(chosen, count) =>
                                    apply((current) => updateHost(current, { ...at, host: chosen }, { count }))
                                }
                            />
                            {host && (
                                <HostInspector
                                    host={host}
                                    onChange={(next) =>
                                        apply((current) => updateHost(current, { ...at, host: selectedHost }, next))
                                    }
                                />
                            )}
                        </Stack>
                    </Tabs.Panel>
                </ScrollArea>
            </Tabs>
        </Stack>
    )
}
