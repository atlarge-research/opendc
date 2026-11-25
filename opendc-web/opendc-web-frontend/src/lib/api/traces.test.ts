import { importLanded } from "@/lib/api/traces"
import type { ImportProgress, TraceImport } from "@/lib/api/types"
import { describe, expect, it } from "vitest"

function imported(id: string, progress: ImportProgress): TraceImport {
    return { id, traceId: `trace-${id}`, slug: `me/${id}`, kind: "workload", progress }
}

const running: ImportProgress = { type: "running", since: "t0" }
const succeeded: ImportProgress = { type: "succeeded", at: "t1" }
const failed: ImportProgress = { type: "failed", at: "t1", reason: "404" }

describe("importLanded", () => {
    it("notices an import that was running and has since succeeded", () => {
        expect(importLanded([imported("a", running)], [imported("a", succeeded)])).toBe(true)
    })

    it("ignores one that failed, which adds nothing to the library", () => {
        expect(importLanded([imported("a", running)], [imported("a", failed)])).toBe(false)
    })

    it("ignores one that had already succeeded before, so a settled list does not refetch forever", () => {
        expect(importLanded([imported("a", succeeded)], [imported("a", succeeded)])).toBe(false)
    })

    it("ignores one seen for the first time already succeeded, since the library was loaded after it", () => {
        expect(importLanded([], [imported("a", succeeded)])).toBe(false)
    })
})
