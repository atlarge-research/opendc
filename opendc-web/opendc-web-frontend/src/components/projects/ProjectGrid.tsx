"use client"

import { openNamePrompt } from "@/components/util/NamePrompt"
import { notifyProblem } from "@/components/util/feedback"
import { useCreateProject } from "@/lib/api/projects"
import type { Project } from "@/lib/api/types"
import { Card, Center, SimpleGrid, Stack, Text, UnstyledButton } from "@mantine/core"
import { IconPlus } from "@tabler/icons-react"
import { useRouter } from "next/navigation"
import { ProjectCard } from "./ProjectCard"

export function ProjectGrid({ projects, emptyMessage }: { projects: Project[]; emptyMessage: string }) {
    return (
        <Stack gap="md">
            {projects.length === 0 && (
                <Text c="dimmed" size="sm">
                    {emptyMessage}
                </Text>
            )}
            <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
                {projects.map((project) => (
                    <ProjectCard key={project.id} project={project} />
                ))}
                <CreateProjectCard />
            </SimpleGrid>
        </Stack>
    )
}

function CreateProjectCard() {
    const create = useCreateProject()
    const router = useRouter()

    const promptCreate = () =>
        openNamePrompt({
            title: "Create a project",
            label: "Project name",
            confirmLabel: "Create",
            onSubmit: (name) =>
                create.mutate(name, {
                    onSuccess: (project) => router.push(`/project?id=${project.id}`),
                    onError: notifyProblem,
                }),
        })

    return (
        <UnstyledButton onClick={promptCreate}>
            <Card withBorder radius="md" padding="md" h="100%" style={{ borderStyle: "dashed" }}>
                <Center mih={96}>
                    <Stack align="center" gap={4}>
                        <IconPlus size={22} />
                        <Text size="sm" c="dimmed">
                            Create project
                        </Text>
                    </Stack>
                </Center>
            </Card>
        </UnstyledButton>
    )
}
