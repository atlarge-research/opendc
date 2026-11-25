import type { Id } from "@/lib/api/types"

export type IdParam = { status: "ok"; value: Id } | { status: "missing" }

export interface ReadableParams {
    get(name: string): string | null
}

// Ids are spliced into API paths, and a link can carry anything, so "../me" must not reach another endpoint.
const PATH_SAFE_ID = /^[\w-]+$/

/** Reads an entity id from the query string. Ids are opaque: only a path-safe alphabet is checked here. */
export function idParam(params: ReadableParams, name: string): IdParam {
    const raw = params.get(name)?.trim()
    if (raw === undefined || !PATH_SAFE_ID.test(raw)) return { status: "missing" }
    return { status: "ok", value: raw }
}
