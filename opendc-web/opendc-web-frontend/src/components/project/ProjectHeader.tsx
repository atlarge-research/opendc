"use client"

import { EntityBreadcrumbs } from "@/components/util/EntityBreadcrumbs"
import { openNamePrompt } from "@/components/util/NamePrompt"
import { notifyProblem } from "@/components/util/feedback"
import { useRenameProject } from "@/lib/api/projects"
import type { Project } from "@/lib/api/types"
import { allows } from "@/lib/project/permissions"
import { ActionIcon, Badge, Group } from "@mantine/core"
import { IconPencil } from "@tabler/icons-react"

/** The project's breadcrumb trail with the caller's role and, for editors, a button to rename it. */
export function ProjectHeader({ project }: Readonly<{ project: Project }>) {
    const rename = useRenameProject(project.id)

    const promptRename = () =>
        openNamePrompt({
            title: "Rename project",
            label: "Project name",
            initial: project.name,
            confirmLabel: "Rename",
            onSubmit: (name) => rename.mutate(name, { onError: notifyProblem }),
        })

    return (
        <EntityBreadcrumbs
            crumbs={[
                { kind: "projects", label: "Projects", href: "/" },
                { kind: "project", label: project.name },
            ]}
            trailing={
                <Group gap={6} wrap="nowrap">
                    <Badge variant="light" color="opendc" tt="capitalize" size="sm">
                        {project.role}
                    </Badge>
                    {allows(project.role, "edit") && (
                        <ActionIcon
                            variant="subtle"
                            color="gray"
                            size="sm"
                            aria-label="Rename project"
                            onClick={promptRename}
                        >
                            <IconPencil size={14} />
                        </ActionIcon>
                    )}
                </Group>
            }
        />
    )
}
