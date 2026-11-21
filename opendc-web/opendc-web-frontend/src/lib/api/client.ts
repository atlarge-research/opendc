import type { ApiProblem } from "@/lib/api/types"
import { config } from "@/lib/config"

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE"

export const UNREACHABLE_TITLE = "Cannot reach OpenDC"

export interface RequestOptions {
    method?: HttpMethod
    body?: unknown
}

/** Where a request's credentials come from: an access token in Auth0 mode, nothing otherwise. */
export type AuthHeaders = () => Promise<Record<string, string>>

let authHeaders: AuthHeaders = async () => ({})

/** Registers the source of every request's credentials, once the session knows how to sign in. */
export function setAuthHeaders(source: AuthHeaders): void {
    authHeaders = source
}

/** A server path as the browser reaches it, which is the same origin unless a build says otherwise. */
export function apiUrl(path: string): string {
    return `${config.apiBaseUrl}/${path}`
}

/** The headers that prove who is asking. A token that cannot be had is a signed-out caller. */
export async function credentials(): Promise<Record<string, string>> {
    try {
        return await authHeaders()
    } catch {
        throw new ApiError({ status: 401, title: "Sign in to continue", issues: [] })
    }
}

export class ApiError extends Error {
    readonly problem: ApiProblem

    constructor(problem: ApiProblem) {
        super(problem.title)
        this.name = "ApiError"
        this.problem = problem
    }
}

export function problemOf(error: unknown): ApiProblem {
    if (error instanceof ApiError) return error.problem
    const title = error instanceof Error ? error.message : "Something went wrong"
    return { status: 0, title, issues: [] }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const { method = "GET", body } = options
    // A content type only with a body: the server matches a bodyless request with one against
    // endpoints that expect JSON and refuses it.
    const headers: Record<string, string> = {
        ...(await credentials()),
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
    }
    const response = await fetch(apiUrl(path), {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
    })
    if (!response.ok) {
        throw new ApiError(await problemFrom(response))
    }
    if (response.status === 204) {
        return undefined as T
    }
    return (await response.json()) as T
}

/** A plain-text resource, such as a log, with failures read the same way as JSON requests. */
export async function apiText(path: string): Promise<string> {
    const response = await fetch(apiUrl(path), { headers: await credentials() })
    if (!response.ok) {
        throw new ApiError(await problemFrom(response))
    }
    return response.text()
}

async function problemFrom(response: Response): Promise<ApiProblem> {
    const fallback: ApiProblem = {
        status: response.status,
        title: `Request failed with status ${response.status}`,
        issues: [],
    }
    try {
        const payload = (await response.json()) as Partial<ApiProblem>
        if (typeof payload?.title !== "string") return fallback
        return {
            status: payload.status ?? response.status,
            title: payload.title,
            detail: payload.detail,
            issues: Array.isArray(payload.issues) ? payload.issues : [],
        }
    } catch {
        return fallback
    }
}
