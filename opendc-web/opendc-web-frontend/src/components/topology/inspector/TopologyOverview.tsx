"use client"

import { formatCount, formatMemory, formatPower } from "@/components/format"
import { ShortcutGuide } from "@/components/topology/ShortcutGuide"
import { IssueList } from "@/components/topology/inspector/IssueList"
import type { DocumentIssue } from "@/lib/api/types"
import { overBudgetDataCenters, topologyCapacity } from "@/lib/topology/capacity"
import type { TopologySpec } from "@/lib/topology/spec"
import { Divider, Group, ScrollArea, Stack, Text } from "@mantine/core"

export function TopologyOverview({ topology, issues }: { topology: TopologySpec; issues: DocumentIssue[] }) {
    const capacity = topologyCapacity(topology)
    const overBudget = overBudgetDataCenters(topology).length > 0
    const stats = [
        { label: "Data centers", value: formatCount(capacity.dataCenters) },
        { label: "Clusters", value: formatCount(capacity.clusters) },
        { label: "Hosts", value: formatCount(capacity.hosts) },
        { label: "Cores", value: formatCount(capacity.cores) },
        ...(capacity.gpus > 0 ? [{ label: "GPUs", value: formatCount(capacity.gpus) }] : []),
        { label: "Memory", value: formatMemory(capacity.memoryMiB) },
    ]

    return (
        <ScrollArea h="100%" p="md">
            <Stack gap="sm">
                <Text fw={600}>Topology</Text>
                <Stack gap={4}>
                    {stats.map((stat) => (
                        <Group key={stat.label} justify="space-between">
                            <Text size="sm" c="dimmed">
                                {stat.label}
                            </Text>
                            <Text size="sm" fw={500}>
                                {stat.value}
                            </Text>
                        </Group>
                    ))}
                    <Group justify="space-between">
                        <Text size="sm" c="dimmed">
                            Peak draw
                        </Text>
                        <Text size="sm" fw={500} c={overBudget ? "red" : undefined}>
                            {formatPower(capacity.peakPowerW)}
                        </Text>
                    </Group>
                </Stack>
                {overBudget && (
                    <Text size="xs" c="red">
                        A data center draws more than its supply allows.
                    </Text>
                )}
                <IssueList issues={issues} />
                <Divider />
                <ShortcutGuide />
            </Stack>
        </ScrollArea>
    )
}
