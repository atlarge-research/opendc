import type { ApiProblem } from "@/lib/api/types"

/** The API's origin, set at build time; empty means the origin that serves the app. */
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? ""

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE"

export const UNREACHABLE_TITLE = "Cannot reach OpenDC"

export interface RequestOptions {
    method?: HttpMethod
    body?: unknown
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

/** Where a request's credentials come from: an access token in Auth0 mode, nothing otherwise. */
export type AuthHeaders = () => Promise<Record<string, string>>

let authHeaders: AuthHeaders = async () => ({})

/** Called by the session once it knows whether requests carry a token. */
export function setAuthHeaders(source: AuthHeaders): void {
    authHeaders = source
}

/** A server path as the browser requests it: the same origin, unless the build sets an API base URL. */
export function apiUrl(path: string): string {
    return `${API_BASE_URL}/${path}`
}

/** The headers that prove who is asking. A token that cannot be had is a signed-out caller. */
export async function credentials(): Promise<Record<string, string>> {
    try {
        return await authHeaders()
    } catch {
        throw new ApiError({ status: 401, title: "Sign in to continue", issues: [] })
    }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const { method = "GET", body } = options
    // A bodyless request must not declare JSON, or the server tries to parse the empty body.
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
