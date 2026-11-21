import { apiRequest } from "@/lib/api/client"
import type { AccessToken, MintedToken, UserProfile } from "@/lib/api/types"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

// The profile key is a prefix of the others, so invalidating it refreshes everything about the caller.
export const meKeys = {
    profile: ["me"] as const,
    billing: ["me", "billing"] as const,
    tokens: ["me", "tokens"] as const,
}

export interface ProfileChange {
    handle: string
    displayName: string
}

export function useUpdateProfile() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (change: ProfileChange) =>
            apiRequest<UserProfile>("api/v1/me/profile", { method: "PUT", body: change }),
        onSuccess: (profile) => queryClient.setQueryData(meKeys.profile, profile),
    })
}

export function useDeactivateAccount() {
    return useMutation({ mutationFn: () => apiRequest<void>("api/v1/me", { method: "DELETE" }) })
}

export function useAccessTokens(enabled: boolean) {
    return useQuery({
        queryKey: meKeys.tokens,
        queryFn: () => apiRequest<AccessToken[]>("api/v1/me/tokens"),
        enabled,
    })
}

export function useMintToken() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (name: string) => apiRequest<MintedToken>("api/v1/me/tokens", { method: "POST", body: { name } }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: meKeys.tokens }),
    })
}

export function useRevokeToken() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (id: string) => apiRequest<void>(`api/v1/me/tokens/${id}`, { method: "DELETE" }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: meKeys.tokens }),
    })
}
