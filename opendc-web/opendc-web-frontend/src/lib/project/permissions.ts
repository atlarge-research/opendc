import { useProject } from "@/lib/api/projects"
import type { Id, ProjectRole } from "@/lib/api/types"

/** What someone needs to be allowed to do in a project; the server decides, this only hides what it would refuse. */
export type ProjectPermission = "read" | "edit" | "manage"

export function allows(role: ProjectRole, permission: ProjectPermission): boolean {
    switch (role) {
        case "owner":
            return true
        case "editor":
            return permission !== "manage"
        case "viewer":
            return permission === "read"
    }
}

/** Whether the caller may do [permission] in the project, and no while that is not yet known. */
export function usePermission(projectId: Id, permission: ProjectPermission): boolean {
    const project = useProject(projectId)
    return project.data !== undefined && allows(project.data.role, permission)
}
