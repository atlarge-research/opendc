"use client"

import { TopologyTable } from "@/components/project/TopologyTable"
import { newTopology } from "@/components/topology/defaults"
import { openNamePrompt } from "@/components/util/NamePrompt"
import { notifyProblem } from "@/components/util/feedback"
import { useCreateTopology } from "@/lib/api/topologies"
import type { Id, TopologyTemplate } from "@/lib/api/types"
import { usePermission } from "@/lib/project/permissions"
import { Button, FileButton, Group, Paper, Stack, Title } from "@mantine/core"
import { IconPlus, IconUpload } from "@tabler/icons-react"

export function TopologySection({ projectId, templates }: { projectId: Id; templates: TopologyTemplate[] }) {
    const create = useCreateTopology(projectId)
    const canEdit = usePermission(projectId, "edit")

    const promptCreate = () =>
        openNamePrompt({
            title: "Create a topology",
            label: "Topology name",
            confirmLabel: "Create",
            onSubmit: (name) => create.mutate({ name, topology: newTopology() }, { onError: notifyProblem }),
        })

    // The server validates, and converts a legacy document with the simulator's own conversion.
    const importFile = async (file: File | null) => {
        if (!file) return
        try {
            const document: unknown = JSON.parse(await file.text())
            if (typeof document !== "object" || document === null || Array.isArray(document)) {
                throw new Error(`${file.name} does not hold a topology`)
            }
            create.mutate({ name: file.name.replace(/\.json$/i, ""), topology: document }, { onError: notifyProblem })
        } catch (error) {
            notifyProblem(error)
        }
    }

    return (
        <Paper withBorder radius="md" p="md">
            <Stack gap="sm">
                <Group justify="space-between">
                    <Title order={4}>Topologies</Title>
                    {canEdit && (
                        <Group gap="xs">
                            <FileButton onChange={importFile} accept="application/json">
                                {(props) => (
                                    <Button {...props} variant="default" leftSection={<IconUpload size={16} />}>
                                        Import JSON
                                    </Button>
                                )}
                            </FileButton>
                            <Button
                                leftSection={<IconPlus size={16} />}
                                onClick={promptCreate}
                                loading={create.isPending}
                            >
                                New topology
                            </Button>
                        </Group>
                    )}
                </Group>
                <TopologyTable projectId={projectId} templates={templates} />
            </Stack>
        </Paper>
    )
}
