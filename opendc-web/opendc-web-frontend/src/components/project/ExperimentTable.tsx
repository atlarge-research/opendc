"use client"

import { ExperimentStateBadge } from "@/components/experiment/ExperimentStateBadge"
import { ProgressMeter } from "@/components/experiment/ProgressMeter"
import { formatCount, formatUpdatedAt } from "@/components/format"
import { notifyProblem } from "@/components/util/feedback"
import { useCloneExperiment, useDeleteExperiment } from "@/lib/api/experiments"
import type { ExperimentSummary, Id } from "@/lib/api/types"
import { progressFraction } from "@/lib/experiment/status"
import { usePermission } from "@/lib/project/permissions"
import { ActionIcon, Anchor, Group, Menu, Table, Text } from "@mantine/core"
import { modals } from "@mantine/modals"
import { IconCopy, IconDots, IconTrash } from "@tabler/icons-react"
import Link from "next/link"

export function ExperimentTable({
    projectId,
    experiments,
}: {
    projectId: Id
    experiments: ExperimentSummary[]
}) {
    if (experiments.length === 0) {
        return (
            <Text c="dimmed" size="sm">
                No experiments yet. Create one to sweep topologies, workloads and schedulers.
            </Text>
        )
    }

    return (
        <Table.ScrollContainer minWidth={640}>
            <Table highlightOnHover verticalSpacing="sm">
                <Table.Thead>
                    <Table.Tr>
                        <Table.Th>Name</Table.Th>
                        <Table.Th>State</Table.Th>
                        <Table.Th>Scenarios</Table.Th>
                        <Table.Th w={180}>Progress</Table.Th>
                        <Table.Th>Created</Table.Th>
                        <Table.Th />
                    </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                    {experiments.map((experiment) => (
                        <ExperimentRow key={experiment.id} projectId={projectId} experiment={experiment} />
                    ))}
                </Table.Tbody>
            </Table>
        </Table.ScrollContainer>
    )
}

function ExperimentRow({ projectId, experiment }: { projectId: Id; experiment: ExperimentSummary }) {
    return (
        <Table.Tr>
            <Table.Td>
                <Anchor component={Link} href={`/experiment?id=${experiment.id}`} fw={500}>
                    {experiment.name}
                </Anchor>
            </Table.Td>
            <Table.Td>
                <ExperimentStateBadge state={experiment.state} />
            </Table.Td>
            <Table.Td>{formatCount(experiment.scenarioCount)}</Table.Td>
            <Table.Td>
                {experiment.state === "draft" ? (
                    <Text size="sm" c="dimmed">
                        Not submitted
                    </Text>
                ) : (
                    <ProgressMeter
                        fraction={progressFraction(experiment.progress)}
                        failed={experiment.state === "failed"}
                        width={100}
                        label={`${experiment.name} progress`}
                    />
                )}
            </Table.Td>
            <Table.Td>
                <Text size="sm" c="dimmed">
                    {formatUpdatedAt(experiment.createdAt)}
                </Text>
            </Table.Td>
            <Table.Td>
                <ExperimentRowActions projectId={projectId} experiment={experiment} />
            </Table.Td>
        </Table.Tr>
    )
}

function ExperimentRowActions({ projectId, experiment }: { projectId: Id; experiment: ExperimentSummary }) {
    const clone = useCloneExperiment(experiment.id)
    const remove = useDeleteExperiment(projectId)
    const canEdit = usePermission(projectId, "edit")

    const confirmDelete = () =>
        modals.openConfirmModal({
            title: `Delete ${experiment.name}?`,
            children: <Text size="sm">Its results and scenario history are removed with it.</Text>,
            labels: { confirm: "Delete experiment", cancel: "Keep it" },
            confirmProps: { color: "red" },
            onConfirm: () => remove.mutate(experiment.id, { onError: notifyProblem }),
        })

    if (!canEdit) return null

    return (
        <Group justify="flex-end">
            <Menu position="bottom-end">
                <Menu.Target>
                    <ActionIcon variant="subtle" color="gray" aria-label={`Actions for ${experiment.name}`}>
                        <IconDots size={18} />
                    </ActionIcon>
                </Menu.Target>
                <Menu.Dropdown>
                    <Menu.Item
                        leftSection={<IconCopy size={16} />}
                        onClick={() => clone.mutate(undefined, { onError: notifyProblem })}
                    >
                        Clone to draft
                    </Menu.Item>
                    <Menu.Item color="red" leftSection={<IconTrash size={16} />} onClick={confirmDelete}>
                        Delete
                    </Menu.Item>
                </Menu.Dropdown>
            </Menu>
        </Group>
    )
}
