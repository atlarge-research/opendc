import { apiRequest } from "@/lib/api/client"
import type { CatalogEntry, CatalogName, HostTemplate } from "@/lib/api/types"
import { useQuery } from "@tanstack/react-query"

export const catalogKeys = {
    detail: (catalog: CatalogName | "host-templates") => ["catalogs", catalog] as const,
}

// Catalogs are reflected from the simulator's own types, so they cannot change while the server
// runs. Traces are not among them: they are a library people add to, and they live in traces.ts.
export function useCatalog(catalog: CatalogName) {
    return useQuery({
        queryKey: catalogKeys.detail(catalog),
        queryFn: () => apiRequest<CatalogEntry[]>(`api/v1/catalogs/${catalog}`),
        staleTime: Number.POSITIVE_INFINITY,
    })
}

export function useHostTemplates() {
    return useQuery({
        queryKey: catalogKeys.detail("host-templates"),
        queryFn: () => apiRequest<HostTemplate[]>("api/v1/catalogs/host-templates"),
        staleTime: Number.POSITIVE_INFINITY,
    })
}
