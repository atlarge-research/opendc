import { ApiError, UNREACHABLE_TITLE, apiUrl } from "@/lib/api/client"
import type { DeploymentConfig } from "@/lib/api/types"
import { useQuery } from "@tanstack/react-query"

/** How this deployment signs people in, fetched once and without credentials since it decides them. */
export function useDeploymentConfig() {
    return useQuery({
        queryKey: ["config"],
        queryFn: async () => {
            const response = await fetch(apiUrl("api/v1/config"))
            if (!response.ok) {
                throw new ApiError({ status: response.status, title: UNREACHABLE_TITLE, issues: [] })
            }
            return (await response.json()) as DeploymentConfig
        },
        staleTime: Number.POSITIVE_INFINITY,
        retry: false,
    })
}
