import { apiRequest } from "@/lib/api/client"
import type { Id, TopologyTemplate } from "@/lib/api/types"
import type { FloorPlan } from "@/lib/topology/layout"
import type { TopologySpec } from "@/lib/topology/spec"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

// A topology is addressed by its own id alone; the project only scopes the list it appears in.
export const topologyKeys = {
    list: (projectId: Id) => ["projects", projectId, "topologies"] as const,
    detail: (templateId: Id) => ["topologies", templateId] as const,
}

/**
 * What a new topology is created from. The server is the validator and converts a legacy document
 * itself, so an imported file is sent as it was read.
 */
export interface TopologyDraft {
    name: string
    topology: unknown
}

export function useTopologies(projectId: Id) {
    return useQuery({
        queryKey: topologyKeys.list(projectId),
        queryFn: () => apiRequest<TopologyTemplate[]>(`api/v1/topologies?project=${projectId}`),
        retry: false,
    })
}

export function useTopology(templateId: Id) {
    return useQuery({
        queryKey: topologyKeys.detail(templateId),
        queryFn: () => apiRequest<TopologyTemplate>(`api/v1/topologies/${templateId}`),
        retry: false,
    })
}

export function useCreateTopology(projectId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (draft: TopologyDraft) =>
            apiRequest<TopologyTemplate>("api/v1/topologies", {
                method: "POST",
                body: { projectId, name: draft.name, topology: draft.topology },
            }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: topologyKeys.list(projectId) }),
    })
}

export function useDeleteTopology(projectId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (templateId: Id) => apiRequest<void>(`api/v1/topologies/${templateId}`, { method: "DELETE" }),
        onSuccess: (_result, templateId) => {
            queryClient.removeQueries({ queryKey: topologyKeys.detail(templateId) })
            queryClient.invalidateQueries({ queryKey: topologyKeys.list(projectId) })
        },
    })
}

export interface TopologySave {
    templateId: Id
    name: string
    topology: TopologySpec
    layout: FloorPlan
}

export function useSaveTopology(projectId: Id) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (save: TopologySave) =>
            apiRequest<TopologyTemplate>(`api/v1/topologies/${save.templateId}`, {
                method: "PUT",
                body: { name: save.name, topology: save.topology, layout: save.layout },
            }),
        // Seed the detail cache with the saved document instead of invalidating it, or the open
        // editor would unmount while the refetch loads.
        onSuccess: (template) => {
            queryClient.setQueryData(topologyKeys.detail(template.id), template)
            queryClient.invalidateQueries({ queryKey: topologyKeys.list(projectId) })
        },
    })
}
