import { ApiError, apiRequest, apiUrl, credentials } from "@/lib/api/client"
import { traceKeys } from "@/lib/api/traces"
import type { RegisteredTrace, Trace, TraceKind, UploadPart } from "@/lib/api/types"
import { useMutation, useQueryClient } from "@tanstack/react-query"

export interface UploadProgress {
    sent: number
    total: number
}

export interface TraceUpload {
    kind: TraceKind
    name: string
    description: string
    files: Record<string, File>
    onProgress?: (progress: UploadProgress) => void
    /** Stops the transfers and takes the half-made trace back out again. */
    signal?: AbortSignal
}

/**
 * Transfers in flight across all tables. One storage connection settles at a few MB/s, so more
 * parts go faster; the cap is modest because each one is a request the browser holds open.
 */
const CONCURRENT_PARTS = 8

/** One stretch of one table, and the file to cut it from. */
interface Transfer {
    id: string
    part: UploadPart
    file: File
    table: string
    direct: boolean
}

/**
 * Claims the name, puts each table where its slots point, then completes. Direct slots go straight
 * to object storage, so the API never holds the trace's bytes.
 */
export function useUploadTrace() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: async (upload: TraceUpload): Promise<Trace> => {
            // The sizes decide how each file is cut into parts when the slots are handed out.
            const files = Object.entries(upload.files).map(([table, file]) => ({ table, sizeBytes: file.size }))
            const registered = await apiRequest<RegisteredTrace>("api/v1/traces", {
                method: "POST",
                body: { kind: upload.kind, name: upload.name, description: upload.description || undefined, files },
            })

            const transfers = registered.uploads.flatMap((slot): Transfer[] => {
                const file = upload.files[slot.table]
                if (file === undefined) throw missingFile(slot.table)
                return slot.parts.map((part, index) => ({
                    id: `${slot.table}#${index}`,
                    part,
                    file,
                    table: slot.table,
                    direct: slot.direct,
                }))
            })

            const total = transfers.reduce((bytes, { part }) => bytes + part.length, 0)
            const sent = new Map<string, number>()
            const report = (id: string, bytes: number) => {
                sent.set(id, bytes)
                upload.onProgress?.({ sent: [...sent.values()].reduce((a, b) => a + b, 0), total })
            }

            const stop = new AbortController()
            upload.signal?.addEventListener("abort", () => stop.abort(), { once: true })

            try {
                await inParallel(transfers, CONCURRENT_PARTS, async (transfer) => {
                    try {
                        await putPart(transfer, (bytes) => report(transfer.id, bytes), stop.signal)
                    } catch (error) {
                        stop.abort()
                        throw error
                    }
                })
                return await apiRequest<Trace>(`api/v1/traces/${registered.trace.id}/complete`, { method: "POST" })
            } catch (error) {
                await discard(registered.trace.id)
                throw error
            }
        },
        onSuccess: () => queryClient.invalidateQueries({ queryKey: traceKeys.all }),
    })
}

/** Sends at most [limit] transfers at once, starting the next as soon as one finishes. */
async function inParallel(
    transfers: Transfer[],
    limit: number,
    send: (transfer: Transfer) => Promise<void>,
): Promise<void> {
    const queue = [...transfers]
    const worker = async () => {
        for (let transfer = queue.shift(); transfer !== undefined; transfer = queue.shift()) {
            await send(transfer)
        }
    }
    await Promise.all(Array.from({ length: Math.min(limit, queue.length) }, worker))
}

/** XMLHttpRequest rather than fetch, because fetch cannot report upload progress. */
async function putPart(transfer: Transfer, onSent: (bytes: number) => void, signal: AbortSignal): Promise<void> {
    const { part, file, table, direct } = transfer
    // A direct target is a signed absolute URL that accepts only the headers it was signed for, so no
    // content type either way; anything else is an API path that needs the caller's credentials.
    const url = direct ? part.url : apiUrl(part.url)
    const headers = direct ? {} : await credentials()

    return new Promise((resolve, reject) => {
        const failed = (status: number, message: string) =>
            reject(
                new ApiError({
                    status,
                    title: `Could not upload ${table}`,
                    issues: [{ path: table, message }],
                }),
            )

        if (signal.aborted) {
            failed(0, "the upload was cancelled")
            return
        }

        const request = new XMLHttpRequest()
        request.open("PUT", url)
        for (const [name, value] of Object.entries(headers)) request.setRequestHeader(name, value)
        signal.addEventListener("abort", () => request.abort(), { once: true })
        request.upload.addEventListener("progress", (event) => onSent(event.loaded))
        request.addEventListener("load", () => {
            if (request.status < 400) {
                onSent(part.length)
                resolve()
            } else {
                failed(request.status, request.statusText || "the upload was refused")
            }
        })
        // A bucket missing its CORS policy also lands here, with nothing to report.
        request.addEventListener("error", () => failed(0, "could not reach storage"))
        request.addEventListener("abort", () => failed(0, "the upload was cancelled"))
        // A slice is a view, not a copy, so no part of the file is read into memory.
        request.send(file.slice(part.offset, part.offset + part.length))
    })
}

/** Removes a registration whose upload failed, without hiding why the upload failed. */
async function discard(traceId: string): Promise<void> {
    try {
        await apiRequest<void>(`api/v1/traces/${traceId}`, { method: "DELETE" })
    } catch {
        // The caller is about to be told the upload's own error.
    }
}

function missingFile(table: string): ApiError {
    return new ApiError({
        status: 400,
        title: `Choose a file for ${table}`,
        issues: [{ path: table, message: "is required" }],
    })
}
