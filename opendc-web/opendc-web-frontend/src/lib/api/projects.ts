import { apiRequest } from "@/lib/api/client"
import type { Id, Project } from "@/lib/api/types"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

export const projectKeys = {
    all: ["projects"] as const,
    detail: (projectId: Id) => ["projects", projectId] as const,
}

export function useProjects() {
    return useQuery({ queryKey: projectKeys.all, queryFn: () => apiRequest<Project[]>("api/v1/projects") })
}

export function useProject(projectId: Id) {
    return useQuery({
        queryKey: projectKeys.detail(projectId),
        queryFn: () => apiRequest<Project>(`api/v1/projects/${projectId}`),
        retry: false,
    })
}

export function useCreateProject() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (name: string) => apiRequest<Project>("api/v1/projects", { method: "POST", body: { name } }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.all }),
    })
}

export function useRenameProject(projectId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (name: string) =>
            apiRequest<Project>(`api/v1/projects/${projectId}`, { method: "PATCH", body: { name } }),
        onSuccess: (project) => {
            queryClient.setQueryData(projectKeys.detail(projectId), project)
            queryClient.invalidateQueries({ queryKey: projectKeys.all, exact: true })
        },
    })
}

export function useDeleteProject() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (projectId: Id) => apiRequest<void>(`api/v1/projects/${projectId}`, { method: "DELETE" }),
        // Everything under a deleted project goes with it, so its cache entries are removed rather
        // than refetched into 404s.
        onSuccess: (_result, projectId) => {
            queryClient.removeQueries({ queryKey: projectKeys.detail(projectId) })
            queryClient.invalidateQueries({ queryKey: projectKeys.all, exact: true })
        },
    })
}
