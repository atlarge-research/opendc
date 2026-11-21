"use client"

import { formatCount, formatMemory, formatPower, formatUpdatedAt } from "@/components/format"
import { downloadJson } from "@/components/util/download"
import { notifyProblem } from "@/components/util/feedback"
import { useCreateTopology, useDeleteTopology } from "@/lib/api/topologies"
import type { Id, TopologyTemplate } from "@/lib/api/types"
import { usePermission } from "@/lib/project/permissions"
import { topologyCapacity } from "@/lib/topology/capacity"
import { ActionIcon, Anchor, Group, Menu, Table, Text } from "@mantine/core"
import { modals } from "@mantine/modals"
import { IconCopy, IconDots, IconDownload, IconTrash } from "@tabler/icons-react"
import Link from "next/link"

export function TopologyTable({ projectId, templates }: { projectId: Id; templates: TopologyTemplate[] }) {
    if (templates.length === 0) {
        return (
            <Text c="dimmed" size="sm">
                No topologies yet. Create one to start planning a datacenter.
            </Text>
        )
    }

    return (
        <Table.ScrollContainer minWidth={720}>
            <Table highlightOnHover verticalSpacing="sm">
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Name</Table.Th>
                        <Table.Th>Data centers</Table.Th>
                        <Table.Th>Clusters</Table.Th>
                        <Table.Th>Hosts</Table.Th>
                        <Table.Th>Cores</Table.Th>
                        <Table.Th>Memory</Table.Th>
                        <Table.Th>Peak power</Table.Th>
                        <Table.Th>Last edited</Table.Th>
                        <Table.Th />
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {templates.map((template) => (
                        <TopologyRow key={template.id} projectId={projectId} template={template} />
                    ))}
                </Table.Tbody>
            </Table>
        </Table.ScrollContainer>
    )
}

function TopologyRow({ projectId, template }: { projectId: Id; template: TopologyTemplate }) {
    const capacity = topologyCapacity(template.topology)
    return (
        <Table.Tr>
            <Table.Td>
                <Anchor component={Link} href={`/topology?id=${template.id}`} fw={500}>
                    {template.name}
                </Anchor>
            </Table.Td>
            <Table.Td>{formatCount(capacity.dataCenters)}</Table.Td>
            <Table.Td>{formatCount(capacity.clusters)}</Table.Td>
            <Table.Td>{formatCount(capacity.hosts)}</Table.Td>
            <Table.Td>{formatCount(capacity.cores)}</Table.Td>
            <Table.Td>{formatMemory(capacity.memoryMiB)}</Table.Td>
            <Table.Td>{formatPower(capacity.peakPowerW)}</Table.Td>
            <Table.Td>
                <Text size="sm" c="dimmed">
                    {formatUpdatedAt(template.updatedAt)}
                </Text>
            </Table.Td>
            <Table.Td>
                <TopologyRowActions projectId={projectId} template={template} />
            </Table.Td>
        </Table.Tr>
    )
}

function TopologyRowActions({ projectId, template }: { projectId: Id; template: TopologyTemplate }) {
    const duplicate = useCreateTopology(projectId)
    const remove = useDeleteTopology(projectId)
    const canEdit = usePermission(projectId, "edit")

    const confirmDelete = () =>
        modals.openConfirmModal({
            title: `Delete ${template.name}?`,
            children: (
                <Text size="sm">
                    Experiments already submitted keep their own copy of this topology and are unaffected.
                </Text>
            ),
            labels: { confirm: "Delete topology", cancel: "Keep it" },
            confirmProps: { color: "red" },
            onConfirm: () => remove.mutate(template.id, { onError: notifyProblem }),
        })

    return (
        <Group justify="flex-end">
            <Menu position="bottom-end">
                <Menu.Target>
                    <ActionIcon variant="subtle" color="gray" aria-label={`Actions for ${template.name}`}>
                        <IconDots size={18} />
                    </ActionIcon>
                </Menu.Target>
                <Menu.Dropdown>
                    {canEdit && (
                        <Menu.Item
                            leftSection={<IconCopy size={16} />}
                            onClick={() =>
                                duplicate.mutate(
                                    { name: `${template.name} (copy)`, topology: template.topology },
                                    { onError: notifyProblem },
                                )
                            }
                        >
                            Duplicate
                        </Menu.Item>
                    )}
                    <Menu.Item
                        leftSection={<IconDownload size={16} />}
                        onClick={() => downloadJson(template.name, template.topology)}
                    >
                        Download JSON
                    </Menu.Item>
                    {canEdit && (
                        <Menu.Item color="red" leftSection={<IconTrash size={16} />} onClick={confirmDelete}>
                            Delete
                        </Menu.Item>
                    )}
                </Menu.Dropdown>
            </Menu>
        </Group>
    )
}
