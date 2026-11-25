import type { Project } from "@/lib/api/types"
import { create } from "zustand"

export type ProjectFilterValue = "all" | "own" | "shared"

interface ProjectSearchState {
    query: string
    setQuery: (query: string) => void
}

export const useProjectSearch = create<ProjectSearchState>((set) => ({
    query: "",
    setQuery: (query) => set({ query }),
}))

export function isSearching(query: string): boolean {
    return query.trim() !== ""
}

export function filterProjects(projects: Project[], filter: ProjectFilterValue, search: string): Project[] {
    const query = search.trim().toLowerCase()
    if (query !== "") {
        return projects.filter((project) => project.name.toLowerCase().includes(query))
    }
    if (filter === "own") {
        return projects.filter((project) => project.role === "owner")
    }
    if (filter === "shared") {
        return projects.filter((project) => project.role !== "owner")
    }
    return projects
}
