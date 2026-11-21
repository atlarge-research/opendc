import { filterProjects, isSearching, useProjectSearch } from "@/components/projects/projectList"
import type { Project, ProjectRole } from "@/lib/api/types"
import { beforeEach, describe, expect, it } from "vitest"

function project(id: string, name: string, role: ProjectRole): Project {
    return { id, name, role, createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" }
}

const projects = [
    project("capacity-id", "Capacity Study", "owner"),
    project("carbon-id", "Carbon Scheduling", "editor"),
    project("gpu-id", "GPU Sizing", "viewer"),
]

const names = (found: Project[]) => found.map((entry) => entry.name)

describe("filterProjects", () => {
    it("splits owned from shared by role", () => {
        expect(names(filterProjects(projects, "own", ""))).toEqual(["Capacity Study"])
        expect(names(filterProjects(projects, "shared", ""))).toEqual(["Carbon Scheduling", "GPU Sizing"])
        expect(names(filterProjects(projects, "all", ""))).toHaveLength(3)
    })

    it("lets a search override the role filter, so results are never silently hidden", () => {
        expect(names(filterProjects(projects, "own", "carbon"))).toEqual(["Carbon Scheduling"])
    })

    it("matches case insensitively on any part of the name", () => {
        expect(names(filterProjects(projects, "all", "SIZING"))).toEqual(["GPU Sizing"])
        expect(names(filterProjects(projects, "all", "ing"))).toEqual(["Carbon Scheduling", "GPU Sizing"])
    })

    it("treats a whitespace-only query as no query at all", () => {
        expect(names(filterProjects(projects, "own", "   "))).toEqual(["Capacity Study"])
        expect(isSearching("   ")).toBe(false)
    })

    it("returns nothing when a search matches nothing", () => {
        expect(filterProjects(projects, "all", "kubernetes")).toEqual([])
    })
})

describe("useProjectSearch", () => {
    beforeEach(() => useProjectSearch.setState({ query: "" }))

    it("marks the filter as active only once a real query is typed", () => {
        expect(isSearching(useProjectSearch.getState().query)).toBe(false)
        useProjectSearch.getState().setQuery("gpu")
        expect(isSearching(useProjectSearch.getState().query)).toBe(true)
        useProjectSearch.getState().setQuery("")
        expect(isSearching(useProjectSearch.getState().query)).toBe(false)
    })
})
