"use client"

import { formatUpdatedAt } from "@/components/format"
import { ProjectActions } from "@/components/projects/ProjectActions"
import type { Project, ProjectRole } from "@/lib/api/types"
import { logoGradient } from "@/theme/theme"
import { Badge, Card, Group, Text } from "@mantine/core"
import { type Icon, IconEye, IconFolder, IconHome, IconPencil } from "@tabler/icons-react"
import Link from "next/link"
import classes from "./ProjectCard.module.css"

const roleBadges: Record<ProjectRole, Icon> = {
    owner: IconHome,
    editor: IconPencil,
    viewer: IconEye,
}

export function ProjectCard({ project }: Readonly<{ project: Project }>) {
    const RoleIcon = roleBadges[project.role]
    return (
        <Card withBorder radius="md" padding="md" className={classes.card} __vars={{ "--logo-gradient": logoGradient }}>
            <Group justify="space-between" mb="xs">
                <IconFolder size={22} />
                <Group gap="xs">
                    <Badge variant="light" color="opendc" leftSection={<RoleIcon size={14} />}>
                        {project.role}
                    </Badge>
                    <ProjectActions project={project} />
                </Group>
            </Group>
            <Link href={`/project?id=${project.id}`} style={{ color: "inherit", textDecoration: "none" }}>
                <Text fw={600} lineClamp={1}>
                    {project.name}
                </Text>
                <Text size="sm" c="dimmed" mt={4}>
                    Last modified {formatUpdatedAt(project.updatedAt)}
                </Text>
            </Link>
        </Card>
    )
}
