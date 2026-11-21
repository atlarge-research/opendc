"use client"

import { AppShell } from "@/components/layout/AppShell"
import { ProjectGrid } from "@/components/projects/ProjectGrid"
import { ProjectsToolbar } from "@/components/projects/ProjectsToolbar"
import {
    type ProjectFilterValue,
    filterProjects,
    isSearching,
    useProjectSearch,
} from "@/components/projects/projectList"
import { CardGridGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { useProjects } from "@/lib/api/projects"
import { useAuth } from "@/lib/auth/auth"
import { Container, Stack, Text, Title } from "@mantine/core"
import { useState } from "react"

function useWelcomeMessage() {
    const auth = useAuth()
    return auth.status === "signedIn" ? `Welcome, ${auth.session.userName}!` : "Welcome!"
}

export default function HomePage() {
    const [filter, setFilter] = useState<ProjectFilterValue>("all")
    const query = useProjectSearch((state) => state.query)
    const projects = useProjects()
    const greeting = useWelcomeMessage()

    return (
        <AppShell>
            <Container size="lg" py="lg">
                <Stack gap="lg">
                    <Stack gap={4} py={25}>
                        <Title order={1}>{greeting}</Title>
                        <Text c="dimmed">Find all your personal and shared projects.</Text>
                    </Stack>
                    <ProjectsToolbar filter={filter} onFilterChange={setFilter} searchActive={isSearching(query)} />
                    <QueryState query={projects} ghost={<CardGridGhost />}>
                        {(loaded) => (
                            <ProjectGrid
                                projects={filterProjects(loaded, filter, query)}
                                emptyMessage={
                                    isSearching(query)
                                        ? `No projects match "${query.trim()}".`
                                        : "No projects here yet. Create one to get started."
                                }
                            />
                        )}
                    </QueryState>
                </Stack>
            </Container>
        </AppShell>
    )
}
