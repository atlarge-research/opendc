"use client"

import { ExperimentTable } from "@/components/project/ExperimentTable"
import { openNamePrompt } from "@/components/util/NamePrompt"
import { notifyProblem } from "@/components/util/feedback"
import { EXPERIMENT_PAGE_SIZE, useCreateExperiment } from "@/lib/api/experiments"
import type { ExperimentSummary, Id, Page, TopologyTemplate } from "@/lib/api/types"
import type { ExperimentSpec } from "@/lib/experiment/spec"
import { usePermission } from "@/lib/project/permissions"
import { Button, Group, Pagination, Paper, Stack, Title, Tooltip } from "@mantine/core"
import { IconPlus } from "@tabler/icons-react"
import { useRouter } from "next/navigation"

export function ExperimentSection({
    projectId,
    experiments,
    page,
    onPage,
    templates,
}: {
    projectId: Id
    experiments: Page<ExperimentSummary>
    page: number
    onPage: (page: number) => void
    templates: TopologyTemplate[]
}) {
    const pages = Math.ceil(experiments.total / EXPERIMENT_PAGE_SIZE)
    const create = useCreateExperiment(projectId)
    const canEdit = usePermission(projectId, "edit")
    const router = useRouter()
    const first = templates[0]

    const promptCreate = () => {
        if (!first) return
        const spec: ExperimentSpec = {
            topologies: [first.topology],
            workloads: [{ type: "trace", source: { type: "named", name: "bitbrains-small" } }],
        }
        openNamePrompt({
            title: "Create an experiment",
            label: "Experiment name",
            confirmLabel: "Create draft",
            onSubmit: (name) =>
                create.mutate(
                    { name, spec },
                    {
                        onSuccess: (experiment) => router.push(`/experiment?id=${experiment.id}`),
                        onError: notifyProblem,
                    },
                ),
        })
    }

    return (
        <Paper withBorder radius="md" p="md">
            <Stack gap="sm">
                <Group justify="space-between">
                    <Title order={4}>Experiments</Title>
                    {canEdit && (
                        <Tooltip label="Create a topology first" disabled={first !== undefined}>
                            <Button
                                leftSection={<IconPlus size={16} />}
                                onClick={promptCreate}
                                disabled={first === undefined}
                                loading={create.isPending}
                            >
                                New experiment
                            </Button>
                        </Tooltip>
                    )}
                </Group>
                <ExperimentTable projectId={projectId} experiments={experiments.items} />
                {pages > 1 && (
                    <Group justify="flex-end">
                        <Pagination total={pages} value={page} onChange={onPage} size="sm" />
                    </Group>
                )}
            </Stack>
        </Paper>
    )
}
