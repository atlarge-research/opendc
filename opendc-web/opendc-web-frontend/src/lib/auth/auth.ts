import { meKeys } from "@/lib/api/account"
import { ApiError, apiRequest, problemOf } from "@/lib/api/client"
import type { Account, ApiProblem, Handle, UserProfile } from "@/lib/api/types"
import { useQuery } from "@tanstack/react-query"

export interface AuthSession {
    userName: string
    handle: Handle
    account: Account
}

export type AuthState =
    | { status: "loading" }
    | { status: "unavailable"; problem: ApiProblem }
    | { status: "signedOut" }
    | { status: "deactivated" }
    | { status: "needsHandle"; profile: UserProfile }
    | { status: "signedIn"; session: AuthSession }

/**
 * A refused /me means signed out (401) or deactivated (403). Any other failure is its own state, so one
 * failed request cannot leave the app on a spinner; once a profile has loaded, a failed refresh keeps
 * the person signed in.
 */
export function authStateOf(profile: UserProfile | undefined, error: unknown): AuthState {
    if (error !== null && error !== undefined) {
        const status = error instanceof ApiError ? error.problem.status : 0
        if (status === 401) return { status: "signedOut" }
        if (status === 403) return { status: "deactivated" }
        if (profile === undefined) return { status: "unavailable", problem: problemOf(error) }
    }
    if (profile === undefined) return { status: "loading" }

    switch (profile.handle.type) {
        case "provisional":
            return { status: "needsHandle", profile }
        case "chosen":
            return {
                status: "signedIn",
                session: {
                    userName: profile.displayName,
                    handle: profile.handle,
                    account: {
                        plan: profile.plan,
                        isAdmin: profile.isAdmin,
                        projectCount: profile.projectCount,
                        budgets: profile.budgets,
                    },
                },
            }
    }
}

export function useAuth(): AuthState & { retry: () => void } {
    const profile = useQuery({
        queryKey: meKeys.profile,
        queryFn: () => apiRequest<UserProfile>("api/v1/me"),
        retry: false,
    })
    return { ...authStateOf(profile.data, profile.error), retry: () => void profile.refetch() }
}
