import type { Id } from "@/lib/api/types"

export type IdParam = { status: "ok"; value: Id } | { status: "missing" }

export interface ReadableParams {
    get(name: string): string | null
}

/** Reads an entity id from the query string. Ids are opaque, so only presence is checked here. */
export function idParam(params: ReadableParams, name: string): IdParam {
    const raw = params.get(name)?.trim()
    if (raw === undefined || raw === "") return { status: "missing" }
    return { status: "ok", value: raw }
}
