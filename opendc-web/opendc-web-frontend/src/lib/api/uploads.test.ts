import { apiUrl, setAuthHeaders } from "@/lib/api/client"
import type { RegisteredTrace, Trace, UploadSlot } from "@/lib/api/types"
import { type TraceUpload, type UploadProgress, uploadTrace } from "@/lib/api/uploads"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

/** Stands in for XMLHttpRequest. Each test decides how a request ends. */
class FakeRequest extends EventTarget {
    static sent: FakeRequest[] = []
    static onSend: (request: FakeRequest) => void = (request) => request.respond(200)

    readonly upload = new EventTarget()
    readonly headers: Record<string, string> = {}
    url = ""
    status = 0
    statusText = ""

    open(_method: string, url: string) {
        this.url = url
    }

    setRequestHeader(name: string, value: string) {
        this.headers[name] = value
    }

    send() {
        FakeRequest.sent.push(this)
        FakeRequest.onSend(this)
    }

    abort() {
        this.dispatchEvent(new Event("abort"))
    }

    progress(loaded: number) {
        this.upload.dispatchEvent(Object.assign(new Event("progress"), { loaded }))
    }

    respond(status: number) {
        this.status = status
        this.dispatchEvent(new Event("load"))
    }
}

const TRACE: Trace = {
    id: "t1",
    slug: "me/nightly",
    kind: "workload",
    access: "owned",
    sizeBytes: 6,
    tables: [],
    createdAt: "2026-01-01T00:00:00Z",
}

const STORAGE_TASKS = "https://bucket.test/tasks"

const DIRECT_TASKS: UploadSlot = {
    table: "tasks",
    direct: true,
    parts: [
        { url: `${STORAGE_TASKS}?part=1`, offset: 0, length: 3 },
        { url: `${STORAGE_TASKS}?part=2`, offset: 3, length: 3 },
    ],
}

const THROUGH_API_FRAGMENTS: UploadSlot = {
    table: "fragments",
    direct: false,
    parts: [{ url: "api/v1/traces/t1/tables/fragments", offset: 0, length: 6 }],
}

function registration(...uploads: UploadSlot[]): RegisteredTrace {
    return { trace: TRACE, uploads }
}

function upload(overrides: Partial<TraceUpload> = {}): TraceUpload {
    return {
        kind: "workload",
        name: "nightly",
        description: "",
        files: { tasks: new File(["abcdef"], "tasks.parquet"), fragments: new File(["abcdef"], "fragments.parquet") },
        ...overrides,
    }
}

function jsonResponse(body: unknown): Response {
    return new Response(JSON.stringify(body), { status: 200, headers: { "Content-Type": "application/json" } })
}

/** Answers the API as a server would, and lists the requests it saw as "METHOD url". */
function stubApi(registered: RegisteredTrace, onRegistering: () => void = () => {}): string[] {
    const calls: string[] = []
    const answer = async (input: RequestInfo | URL, init?: RequestInit) => {
        const call = `${init?.method ?? "GET"} ${String(input)}`
        calls.push(call)
        if (call.startsWith("POST") && call.endsWith("/traces")) {
            onRegistering()
            return jsonResponse(registered)
        }
        return call.endsWith("/complete") ? jsonResponse(TRACE) : new Response(null, { status: 204 })
    }
    vi.stubGlobal("fetch", vi.fn(answer))
    return calls
}

const sentTo = () => FakeRequest.sent.map((request) => request.url)

const registerCall = `POST ${apiUrl("api/v1/traces")}`
const completeCall = `POST ${apiUrl("api/v1/traces/t1/complete")}`
const discardCall = `DELETE ${apiUrl("api/v1/traces/t1")}`

beforeEach(() => {
    vi.stubGlobal("XMLHttpRequest", FakeRequest)
})

afterEach(() => {
    vi.unstubAllGlobals()
    FakeRequest.sent = []
    FakeRequest.onSend = (request) => request.respond(200)
    setAuthHeaders(async () => ({}))
})

describe("uploading a trace", () => {
    it("completes once every part is sent, reporting the bytes as they go", async () => {
        const calls = stubApi(registration(DIRECT_TASKS))
        FakeRequest.onSend = (request) => {
            request.progress(1)
            request.respond(200)
        }
        const progress: UploadProgress[] = []

        const trace = await uploadTrace(upload({ onProgress: (update) => progress.push(update) }))

        expect(trace).toEqual(TRACE)
        expect(calls).toEqual([registerCall, completeCall])
        expect(sentTo()).toEqual([`${STORAGE_TASKS}?part=1`, `${STORAGE_TASKS}?part=2`])
        expect(progress.at(-1)).toEqual({ sent: 6, total: 6 })
    })

    it("keeps the access token away from object storage and gives it to the API", async () => {
        stubApi(registration(DIRECT_TASKS, THROUGH_API_FRAGMENTS))
        setAuthHeaders(async () => ({ Authorization: "Bearer token" }))

        await uploadTrace(upload())

        const headersTo = (url: string) => FakeRequest.sent.find((request) => request.url === url)?.headers
        expect(headersTo(`${STORAGE_TASKS}?part=1`)).toEqual({})
        expect(headersTo(apiUrl("api/v1/traces/t1/tables/fragments"))).toEqual({ Authorization: "Bearer token" })
    })

    it("reports the refused part, not the transfers it cancelled, and takes the trace back out", async () => {
        const calls = stubApi(registration(DIRECT_TASKS))
        FakeRequest.onSend = (request) => {
            if (request.url.endsWith("part=2")) request.respond(403)
        }

        await expect(uploadTrace(upload())).rejects.toMatchObject({ problem: { status: 403 } })

        expect(calls).toEqual([registerCall, discardCall])
    })

    it("stops the transfers and takes the trace back out when cancelled", async () => {
        const calls = stubApi(registration(DIRECT_TASKS))
        const cancel = new AbortController()
        FakeRequest.onSend = () => cancel.abort()

        await expect(uploadTrace(upload({ signal: cancel.signal }))).rejects.toMatchObject({ problem: { status: 0 } })

        expect(calls).toEqual([registerCall, discardCall])
    })

    it("sends nothing when cancelled while the name is still being claimed", async () => {
        const cancel = new AbortController()
        const calls = stubApi(registration(DIRECT_TASKS), () => cancel.abort())

        await expect(uploadTrace(upload({ signal: cancel.signal }))).rejects.toMatchObject({ problem: { status: 0 } })

        expect(FakeRequest.sent).toEqual([])
        expect(calls).toEqual([registerCall, discardCall])
    })

    it("takes the trace back out when the server asks for a table that has no file", async () => {
        const calls = stubApi(registration(DIRECT_TASKS))

        await expect(uploadTrace(upload({ files: {} }))).rejects.toMatchObject({ problem: { status: 400 } })

        expect(calls).toEqual([registerCall, discardCall])
    })
})
