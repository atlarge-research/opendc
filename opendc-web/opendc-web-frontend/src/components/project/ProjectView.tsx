"use client"

import { BackTo, MessagePage } from "@/components/layout/MessagePage"
import { ExperimentSection } from "@/components/project/ExperimentSection"
import { ProjectHeader } from "@/components/project/ProjectHeader"
import { TopologySection } from "@/components/project/TopologySection"
import { LineGhost, TableGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { idParam } from "@/components/util/params"
import { problemOf } from "@/lib/api/client"
import { useExperiments } from "@/lib/api/experiments"
import { useProject } from "@/lib/api/projects"
import { useTopologies } from "@/lib/api/topologies"
import type { Id } from "@/lib/api/types"
import { Container, Stack } from "@mantine/core"
import { useSearchParams } from "next/navigation"
import { useState } from "react"

export function ProjectView() {
    const params = useSearchParams()
    const id = idParam(params, "id")

    if (id.status !== "ok") {
        return (
            <MessagePage title="No project here" message="This link is missing a project id.">
                <BackTo href="/" label="Back to projects" />
            </MessagePage>
        )
    }

    return <LoadedProject projectId={id.value} />
}

function LoadedProject({ projectId }: { projectId: Id }) {
    const project = useProject(projectId)
    const templates = useTopologies(projectId)
    const [page, setPage] = useState(1)
    const experiments = useExperiments(projectId, page)

    // Only a failed first load is a dead end; a failed refetch keeps what is on screen.
    if (project.isError && project.data === undefined) {
        return (
            <MessagePage title="Project not found" message={problemOf(project.error).title}>
                <BackTo href="/" label="Back to projects" />
            </MessagePage>
        )
    }

    return (
        <Container size="lg" py="md">
            <Stack gap="md">
                <QueryState query={project} ghost={<LineGhost width={260} />}>
                    {(loaded) => <ProjectHeader project={loaded} />}
                </QueryState>

                <QueryState query={templates} ghost={<TableGhost columns={8} />}>
                    {(loaded) => <TopologySection projectId={projectId} templates={loaded} />}
                </QueryState>

                <QueryState query={experiments} ghost={<TableGhost columns={6} />}>
                    {(loaded) => (
                        <ExperimentSection
                            projectId={projectId}
                            experiments={loaded}
                            page={page}
                            onPage={setPage}
                            templates={templates.data ?? []}
                        />
                    )}
                </QueryState>
            </Stack>
        </Container>
    )
}
