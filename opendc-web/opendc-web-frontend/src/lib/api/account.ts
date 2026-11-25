import { apiRequest } from "@/lib/api/client"
import type { AccessToken, Billing, MintedToken, UserProfile } from "@/lib/api/types"
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

const MIN_HANDLE_LENGTH = 3

const MAX_HANDLE_LENGTH = 32

/** The server's rule for handles: a letter first, no dash last. */
export const HANDLE_PATTERN = new RegExp(`^[a-z][a-z0-9-]{${MIN_HANDLE_LENGTH - 2},${MAX_HANDLE_LENGTH - 2}}[a-z0-9]$`)

export const HANDLE_RULE = `${MIN_HANDLE_LENGTH} to ${MAX_HANDLE_LENGTH} lowercase letters, digits or dashes, starting with a letter`

/** A handle offered to someone choosing one; reserved names are left to the server. */
export function suggestHandle(nickname: string): string {
    let handle = nickname
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "")
    if (!/^[a-z]/.test(handle)) handle = `u-${handle}`
    handle = handle.slice(0, MAX_HANDLE_LENGTH).replace(/-+$/, "")
    while (handle.length < MIN_HANDLE_LENGTH) handle = `${handle}0`
    return handle
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

export function useBilling(enabled: boolean) {
    return useQuery({
        queryKey: meKeys.billing,
        queryFn: () => apiRequest<Billing>("api/v1/me/billing"),
        enabled,
    })
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
