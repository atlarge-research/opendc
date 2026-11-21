"use client"

import { type ProjectFilterValue, useProjectSearch } from "@/components/projects/projectList"
import type { Project } from "@/lib/api/types"
import { CloseButton, Group, SegmentedControl, TextInput, useComputedColorScheme } from "@mantine/core"
import { IconSearch } from "@tabler/icons-react"

const options = [
    { label: "All projects", value: "all" },
    { label: "My projects", value: "own" },
    { label: "Shared with me", value: "shared" },
]

export function ProjectsToolbar({
    filter,
    onFilterChange,
    searchActive,
}: {
    filter: ProjectFilterValue
    onFilterChange: (value: ProjectFilterValue) => void
    searchActive: boolean
}) {
    return (
        <Group justify="space-between">
            <ProjectFilter value={filter} onChange={onFilterChange} disabled={searchActive} />
            <ProjectSearch />
        </Group>
    )
}

function ProjectFilter({
    value,
    onChange,
    disabled,
}: {
    value: ProjectFilterValue
    onChange: (value: ProjectFilterValue) => void
    disabled: boolean
}) {
    const scheme = useComputedColorScheme()
    return (
        <SegmentedControl
            color={scheme === "dark" ? "gray.7" : "gray.4"}
            autoContrast
            value={value}
            onChange={(next) => onChange(next as ProjectFilterValue)}
            data={options}
            disabled={disabled}
            p={0}
        />
    )
}

function ProjectSearch() {
    const query = useProjectSearch((state) => state.query)
    const setQuery = useProjectSearch((state) => state.setQuery)
    return (
        <TextInput
            value={query}
            onChange={(event) => setQuery(event.currentTarget.value)}
            placeholder="Search projects"
            aria-label="Search projects"
            size="xs"
            w={240}
            leftSection={<IconSearch size={16} />}
            rightSection={
                query === "" ? null : <CloseButton size="sm" aria-label="Clear search" onClick={() => setQuery("")} />
            }
            rightSectionPointerEvents="all"
        />
    )
}
