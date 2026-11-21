"use client"

import { ExperimentActions } from "@/components/experiment/ExperimentActions"
import { ExperimentHelp } from "@/components/experiment/ExperimentHelp"
import { ExperimentOverview } from "@/components/experiment/ExperimentOverview"
import { ExperimentStateBadge } from "@/components/experiment/ExperimentStateBadge"
import { ResultsPanel } from "@/components/experiment/results/ResultsPanel"
import { BackTo, MessagePage } from "@/components/layout/MessagePage"
import { EntityBreadcrumbs } from "@/components/util/EntityBreadcrumbs"
import { PanelGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { idParam } from "@/components/util/params"
import { problemOf } from "@/lib/api/client"
import { useExperiment } from "@/lib/api/experiments"
import { useProject } from "@/lib/api/projects"
import type { Experiment, Id } from "@/lib/api/types"
import { Container, Group, Stack, Tabs } from "@mantine/core"
import { IconChartLine, IconClipboardList, IconHelp } from "@tabler/icons-react"
import { useRouter, useSearchParams } from "next/navigation"

const TABS = ["overview", "results", "help"] as const

type TabName = (typeof TABS)[number]

export function ExperimentView() {
    const params = useSearchParams()
    const experimentId = idParam(params, "id")

    // The link carries no project id: the experiment names its own, so the two cannot disagree.
    if (experimentId.status !== "ok") {
        return (
            <MessagePage title="No experiment here" message="This link is missing an experiment id.">
                <BackTo href="/" label="Back to projects" />
            </MessagePage>
        )
    }

    return <LoadedExperiment experimentId={experimentId.value} tab={tabOf(params.get("tab"))} />
}

function tabOf(raw: string | null): TabName {
    return TABS.find((name) => name === raw) ?? "overview"
}

function LoadedExperiment({ experimentId, tab }: { experimentId: Id; tab: TabName }) {
    const experiment = useExperiment(experimentId)
    const router = useRouter()

    const openTab = (name: string | null) =>
        router.replace(`/experiment?id=${experimentId}&tab=${tabOf(name)}`, { scroll: false })

    // Only a failed first load is a dead end; a failed refetch keeps what is on screen.
    if (experiment.isError && experiment.data === undefined) {
        return (
            <MessagePage title="Experiment not found" message={problemOf(experiment.error).title}>
                <BackTo href="/" label="Back to projects" />
            </MessagePage>
        )
    }

    return (
        <Container size="xl" py="md">
            <QueryState query={experiment} ghost={<PanelGhost height={320} />}>
                {(loaded) => (
                    <Stack gap="md">
                        <Group justify="space-between" wrap="wrap" gap="xs">
                            <ExperimentBreadcrumbs experiment={loaded} />
                            <ExperimentActions experiment={loaded} />
                        </Group>

                        <Tabs value={tab} onChange={openTab} keepMounted={false}>
                            <Tabs.List mb="md">
                                <Tabs.Tab value="overview" leftSection={<IconClipboardList size={16} />}>
                                    Overview
                                </Tabs.Tab>
                                {loaded.state !== "draft" && (
                                    <Tabs.Tab value="results" leftSection={<IconChartLine size={16} />}>
                                        Results
                                    </Tabs.Tab>
                                )}
                                {loaded.state === "draft" && (
                                    <Tabs.Tab value="help" leftSection={<IconHelp size={16} />}>
                                        Help
                                    </Tabs.Tab>
                                )}
                            </Tabs.List>

                            <Tabs.Panel value="overview">
                                <ExperimentOverview experiment={loaded} />
                            </Tabs.Panel>
                            <Tabs.Panel value="results">
                                <ResultsPanel experiment={loaded} />
                            </Tabs.Panel>
                            <Tabs.Panel value="help">
                                <ExperimentHelp spec={loaded.spec} projectId={loaded.projectId} />
                            </Tabs.Panel>
                        </Tabs>
                    </Stack>
                )}
            </QueryState>
        </Container>
    )
}

function ExperimentBreadcrumbs({ experiment }: { experiment: Experiment }) {
    const project = useProject(experiment.projectId)

    return (
        <EntityBreadcrumbs
            crumbs={[
                { kind: "projects", label: "Projects", href: "/" },
                {
                    kind: "project",
                    label: project.data?.name ?? "Project",
                    href: `/project?id=${experiment.projectId}`,
                },
                { kind: "experiment", label: experiment.name },
            ]}
            trailing={<ExperimentStateBadge state={experiment.state} />}
        />
    )
}
