import { HANDLE_PATTERN, suggestHandle } from "@/lib/account/handle"
import { ApiError, apiRequest, setAuthHeaders } from "@/lib/api/client"
import type { UserProfile } from "@/lib/api/types"
import { authStateOf } from "@/lib/auth/auth"
import { afterEach, describe, expect, it, vi } from "vitest"

const PROFILE: UserProfile = {
    displayName: "Ada",
    handle: { type: "chosen", name: "ada" },
    plan: "free",
    isAdmin: false,
    projectCount: 2,
    budgets: [],
}

function problem(status: number): ApiError {
    return new ApiError({ status, title: `status ${status}`, issues: [] })
}

afterEach(() => {
    vi.unstubAllGlobals()
    setAuthHeaders(async () => ({}))
})

describe("suggesting a handle", () => {
    it("makes one the server accepts out of whatever the sign-in called the person", () => {
        for (const nickname of ["Ada Lovelace", "ada.lovelace+test", "42", "", "--x--", "Ünïcødé", "a".repeat(80)]) {
            expect(suggestHandle(nickname)).toMatch(HANDLE_PATTERN)
        }
    })

    it("keeps a usable nickname as it is", () => {
        expect(suggestHandle("Ada-Lovelace")).toBe("ada-lovelace")
        expect(suggestHandle("7of9")).toBe("u-7of9")
    })
})

describe("where a person stands", () => {
    it("tells a deactivated account from one that is signed out", () => {
        expect(authStateOf(undefined, problem(401))).toEqual({ status: "signedOut" })
        expect(authStateOf(undefined, problem(403))).toEqual({ status: "deactivated" })
    })

    it("reads any other failure as the app being unreachable, never as still loading", () => {
        expect(authStateOf(undefined, problem(500)).status).toBe("unavailable")
        expect(authStateOf(undefined, new TypeError("network down")).status).toBe("unavailable")
        expect(authStateOf(undefined, null)).toEqual({ status: "loading" })
    })

    it("holds a first sign-in at choosing a handle", () => {
        const newcomer = { ...PROFILE, handle: { type: "provisional" } as const }
        expect(authStateOf(newcomer, null)).toEqual({ status: "needsHandle", profile: newcomer })
        expect(authStateOf(PROFILE, null).status).toBe("signedIn")
    })
})

describe("calling the API", () => {
    function stubFetch() {
        const fetch = vi.fn(
            async (_input: RequestInfo | URL, _init?: RequestInit) =>
                new Response("{}", { status: 200, headers: { "Content-Type": "application/json" } }),
        )
        vi.stubGlobal("fetch", fetch)
        return fetch
    }

    it("sends the registered credentials, and a content type only with a body", async () => {
        const fetch = stubFetch()
        setAuthHeaders(async () => ({ Authorization: "Bearer token" }))

        await apiRequest("api/v1/projects")
        await apiRequest("api/v1/projects", { method: "POST", body: { name: "p" } })

        const [read, write] = fetch.mock.calls.map(([, init]) => init?.headers)
        expect(read).toEqual({ Authorization: "Bearer token" })
        expect(write).toEqual({ Authorization: "Bearer token", "Content-Type": "application/json" })
    })

    it("reads a token that cannot be had as a signed-out caller, without asking the server", async () => {
        const fetch = stubFetch()
        setAuthHeaders(async () => {
            throw new Error("login required")
        })

        await expect(apiRequest("api/v1/me")).rejects.toMatchObject({ problem: { status: 401 } })
        expect(fetch).not.toHaveBeenCalled()
    })
})
