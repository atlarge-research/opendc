"use client"

import { formatCount, formatSimulationBudget } from "@/components/format"
import { openNamePrompt } from "@/components/util/NamePrompt"
import { downloadJson } from "@/components/util/download"
import { notifyProblem } from "@/components/util/feedback"
import { problemOf } from "@/lib/api/client"
import {
    useCancelExperiment,
    useCloneExperiment,
    useDeleteExperiment,
    useSaveExperimentDraft,
    useSubmitExperiment,
} from "@/lib/api/experiments"
import type { Experiment } from "@/lib/api/types"
import { isTerminalExperiment } from "@/lib/experiment/status"
import { usePermission } from "@/lib/project/permissions"
import { ActionIcon, Button, Group, Menu, Stack, Text } from "@mantine/core"
import { modals } from "@mantine/modals"
import {
    IconCopy,
    IconDots,
    IconDownload,
    IconPencil,
    IconPlayerPlay,
    IconPlayerStop,
    IconTrash,
} from "@tabler/icons-react"
import { useRouter } from "next/navigation"

export function ExperimentActions({ experiment }: { experiment: Experiment }) {
    const submit = useSubmitExperiment(experiment.id)
    const cancel = useCancelExperiment(experiment.id)
    const clone = useCloneExperiment(experiment.id)
    const rename = useSaveExperimentDraft(experiment.id)
    const remove = useDeleteExperiment(experiment.projectId)
    const canEdit = usePermission(experiment.projectId, "edit")
    const router = useRouter()

    const isDraft = experiment.state === "draft"
    const isRunning = !isDraft && !isTerminalExperiment(experiment.state)

    const confirmSubmit = () =>
        modals.openConfirmModal({
            title: `Run ${experiment.name}?`,
            children: (
                <Text size="sm">
                    {formatCount(experiment.estimate.scenarioCount)} scenarios will be queued, reserving about{" "}
                    {formatSimulationBudget(experiment.estimate.estimatedBudgetSeconds)} of budget. The experiment is
                    frozen once it starts.
                </Text>
            ),
            labels: { confirm: "Run experiment", cancel: "Keep editing" },
            // No toast: the error stays under the Run button instead of vanishing on a timer.
            onConfirm: () => submit.mutate(),
        })

    const confirmCancel = () =>
        modals.openConfirmModal({
            title: `Stop ${experiment.name}?`,
            children: <Text size="sm">Scenarios that already finished keep their results. The rest are dropped.</Text>,
            labels: { confirm: "Stop run", cancel: "Let it finish" },
            confirmProps: { color: "red" },
            onConfirm: () => cancel.mutate(undefined, { onError: notifyProblem }),
        })

    const confirmDelete = () =>
        modals.openConfirmModal({
            title: `Delete ${experiment.name}?`,
            children: (
                <Text size="sm">
                    {isRunning
                        ? "Scenarios still running are stopped, and its results and scenario history are removed with it."
                        : "Its results and scenario history are removed with it."}
                </Text>
            ),
            labels: { confirm: "Delete experiment", cancel: "Keep it" },
            confirmProps: { color: "red" },
            onConfirm: () =>
                remove.mutate(experiment.id, {
                    onSuccess: () => router.push(`/project?id=${experiment.projectId}`),
                    onError: notifyProblem,
                }),
        })

    const promptRename = () =>
        openNamePrompt({
            title: "Rename experiment",
            label: "Experiment name",
            initial: experiment.name,
            confirmLabel: "Rename",
            onSubmit: (name) => rename.mutate({ name, spec: experiment.spec }, { onError: notifyProblem }),
        })

    if (!canEdit) return null

    return (
        <Group gap="xs" wrap="nowrap">
            {isDraft && (
                <Stack gap={4} align="flex-end">
                    <Button
                        leftSection={<IconPlayerPlay size={16} />}
                        onClick={confirmSubmit}
                        loading={submit.isPending}
                        disabled={experiment.estimate.scenarioCount === 0}
                    >
                        Run experiment
                    </Button>
                    {submit.isError && (
                        <Text size="xs" c="red.6" maw={260} ta="right">
                            {problemOf(submit.error).title}
                        </Text>
                    )}
                </Stack>
            )}
            {isRunning && (
                <Button
                    variant="light"
                    color="red"
                    leftSection={<IconPlayerStop size={16} />}
                    onClick={confirmCancel}
                    loading={cancel.isPending}
                >
                    Stop
                </Button>
            )}

            <Menu position="bottom-end">
                <Menu.Target>
                    <ActionIcon variant="subtle" color="gray" size="lg" aria-label="Experiment actions">
                        <IconDots size={18} />
                    </ActionIcon>
                </Menu.Target>
                <Menu.Dropdown>
                    {isDraft && (
                        <Menu.Item leftSection={<IconPencil size={16} />} onClick={promptRename}>
                            Rename
                        </Menu.Item>
                    )}
                    <Menu.Item
                        leftSection={<IconCopy size={16} />}
                        onClick={() =>
                            clone.mutate(undefined, {
                                onSuccess: (draft) => router.push(`/experiment?id=${draft.id}`),
                                onError: notifyProblem,
                            })
                        }
                    >
                        Clone to draft
                    </Menu.Item>
                    <Menu.Item
                        leftSection={<IconDownload size={16} />}
                        onClick={() => downloadJson(experiment.name, experiment.spec)}
                    >
                        Export configuration
                    </Menu.Item>
                    <Menu.Divider />
                    <Menu.Item color="red" leftSection={<IconTrash size={16} />} onClick={confirmDelete}>
                        Delete
                    </Menu.Item>
                </Menu.Dropdown>
            </Menu>
        </Group>
    )
}
