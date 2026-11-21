import { apiRequest } from "@/lib/api/client"
import type { CatalogEntry, Trace, TraceImport, TraceKind, TraceKindTables, TraceShare } from "@/lib/api/types"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

export const traceKeys = {
    all: ["traces"] as const,
    ofKind: (kind: TraceKind) => ["traces", kind] as const,
    kinds: ["traces", "kinds"] as const,
    shares: (traceId: string) => ["traces", traceId, "shares"] as const,
}

export function useTraces(kind?: TraceKind) {
    return useQuery({
        queryKey: kind ? traceKeys.ofKind(kind) : traceKeys.all,
        queryFn: () => apiRequest<Trace[]>(kind ? `api/v1/traces?kind=${kind}` : "api/v1/traces"),
    })
}

/** The traces of [kind] a document may reference, as picker options. */
export function useTraceOptions(kind: TraceKind) {
    return useQuery({
        queryKey: traceKeys.ofKind(kind),
        queryFn: () => apiRequest<Trace[]>(`api/v1/traces?kind=${kind}`),
        select: (traces: Trace[]): CatalogEntry[] =>
            traces.map((trace) => ({ id: trace.slug, label: trace.slug, group: kind, description: trace.description })),
    })
}

export function useTraceKinds() {
    return useQuery({
        queryKey: traceKeys.kinds,
        queryFn: () => apiRequest<TraceKindTables[]>("api/v1/traces/kinds"),
        staleTime: Number.POSITIVE_INFINITY,
    })
}

export function useEditTrace() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ id, name, description }: { id: string; name?: string; description?: string }) =>
            apiRequest<Trace>(`api/v1/traces/${id}`, { method: "PATCH", body: { name, description } }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: traceKeys.all }),
    })
}

export function useDeleteTrace() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (id: string) => apiRequest<void>(`api/v1/traces/${id}`, { method: "DELETE" }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: traceKeys.all }),
    })
}

export function useShares(traceId: string, enabled: boolean) {
    return useQuery({
        queryKey: traceKeys.shares(traceId),
        queryFn: () => apiRequest<TraceShare[]>(`api/v1/traces/${traceId}/shares`),
        enabled,
    })
}

export function useShareTrace(traceId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (handle: string) =>
            apiRequest<TraceShare>(`api/v1/traces/${traceId}/shares`, { method: "POST", body: { handle } }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: traceKeys.shares(traceId) }),
    })
}

export function useRevokeShare(traceId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (handle: string) =>
            apiRequest<void>(`api/v1/traces/${traceId}/shares/${handle}`, { method: "DELETE" }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: traceKeys.shares(traceId) }),
    })
}

const IMPORTS_KEY = ["trace-imports"] as const
const IMPORT_POLL_MS = 2000

/** Whether an import that was running in [before] has succeeded by [after], so the library has grown. */
export function importLanded(before: readonly TraceImport[], after: readonly TraceImport[]): boolean {
    const running = new Set(before.filter((entry) => entry.progress.type === "running").map((entry) => entry.id))
    return after.some((entry) => entry.progress.type === "succeeded" && running.has(entry.id))
}

/** The caller's imports, polled while any is running; the library is refetched when one lands. */
export function useTraceImports() {
    const queryClient = useQueryClient()
    return useQuery({
        queryKey: IMPORTS_KEY,
        queryFn: async () => {
            const before = queryClient.getQueryData<TraceImport[]>(IMPORTS_KEY) ?? []
            const after = await apiRequest<TraceImport[]>("api/v1/traces/imports")
            if (importLanded(before, after)) void queryClient.invalidateQueries({ queryKey: traceKeys.all })
            return after
        },
        refetchInterval: (query) =>
            query.state.data?.some((entry) => entry.progress.type === "running") ? IMPORT_POLL_MS : false,
    })
}

export interface ImportRequest {
    kind: TraceKind
    name: string
    description: string
    sources: Record<string, string>
}

export function useStartImport() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (request: ImportRequest) =>
            apiRequest<TraceImport>("api/v1/traces/imports", { method: "POST", body: request }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: IMPORTS_KEY }),
    })
}

export function useDismissImport() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (id: string) => apiRequest<void>(`api/v1/traces/imports/${id}`, { method: "DELETE" }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: IMPORTS_KEY }),
    })
}

/** Where the server signs a link to a trace's tables, as one zip. */
export function traceContentLink(traceId: string): string {
    return `api/v1/traces/${traceId}/content/link`
}
