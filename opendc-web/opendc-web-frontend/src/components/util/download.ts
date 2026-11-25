import { apiRequest, apiUrl } from "@/lib/api/client"
import type { DownloadLink } from "@/lib/api/types"

export function fileSlug(name: string): string {
    return name.replaceAll(/\s+/g, "-").toLowerCase()
}

export function downloadBlob(fileName: string, blob: Blob): void {
    const url = URL.createObjectURL(blob)
    const link = document.createElement("a")
    link.href = url
    link.download = fileName
    link.click()

    // Revoked on the next task: some browsers cancel a download whose URL is revoked during the click.
    setTimeout(() => URL.revokeObjectURL(url))
}

export function downloadText(fileName: string, text: string, type: string): void {
    downloadBlob(fileName, new Blob([text], { type }))
}

export function downloadJson(name: string, value: unknown): void {
    downloadText(`${fileSlug(name)}.json`, JSON.stringify(value, null, 4), "application/json")
}

/**
 * Streams a download from the server straight to disk through a short-lived signed link, since a
 * plain link cannot carry a token. A refused link rejects here, before the browser navigates.
 */
export async function startDownload(linkPath: string): Promise<void> {
    const link = await apiRequest<DownloadLink>(linkPath, { method: "POST" })
    window.location.assign(apiUrl(link.url))
}
