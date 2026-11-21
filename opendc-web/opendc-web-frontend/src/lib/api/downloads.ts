import { apiRequest, apiUrl } from "@/lib/api/client"
import type { DownloadLink } from "@/lib/api/types"

/**
 * Streams a download straight to disk through a short-lived signed link, since a plain link cannot
 * carry a token. A refused link rejects here, before the browser navigates.
 */
export async function startDownload(linkPath: string): Promise<void> {
    const link = await apiRequest<DownloadLink>(linkPath, { method: "POST" })
    window.location.assign(apiUrl(link.url))
}
