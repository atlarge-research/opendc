import { meKeys } from "@/lib/api/account"
import { apiRequest } from "@/lib/api/client"
import { projectKeys } from "@/lib/api/projects"
import type { Id, ProjectRole } from "@/lib/api/types"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

export interface ProjectMember {
    handle: string
    displayName: string
    role: ProjectRole
}

const memberKey = (projectId: Id) => ["projects", projectId, "members"] as const

export function useMembers(projectId: Id, enabled: boolean) {
    return useQuery({
        queryKey: memberKey(projectId),
        queryFn: () => apiRequest<ProjectMember[]>(`api/v1/projects/${projectId}/members`),
        enabled,
    })
}

export function useInviteMember(projectId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (invite: { handle: string; role: ProjectRole }) =>
            apiRequest<ProjectMember>(`api/v1/projects/${projectId}/members`, { method: "POST", body: invite }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: memberKey(projectId) }),
    })
}

export function useChangeRole(projectId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (change: { handle: string; role: ProjectRole }) =>
            apiRequest<ProjectMember>(`api/v1/projects/${projectId}/members/${encodeURIComponent(change.handle)}`, {
                method: "PATCH",
                body: { role: change.role },
            }),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: memberKey(projectId) })
            void queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) })
        },
    })
}

/** Removes someone from a project, or the caller themselves, which is leaving it. */
export function useRemoveMember(projectId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (handle: string) =>
            apiRequest<void>(`api/v1/projects/${projectId}/members/${encodeURIComponent(handle)}`, {
                method: "DELETE",
            }),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: memberKey(projectId) })
            void queryClient.invalidateQueries({ queryKey: projectKeys.all })
            void queryClient.invalidateQueries({ queryKey: meKeys.profile })
        },
    })
}
