import { problemDetail } from "@/components/util/feedback"
import type { ApiProblem } from "@/lib/api/types"
import { describe, expect, it } from "vitest"

function problem(overrides: Partial<ApiProblem>): ApiProblem {
    return { status: 400, title: "Invalid", issues: [], ...overrides }
}

describe("problemDetail", () => {
    it("names each faulted field with the reason, since a path alone says nothing about what to fix", () => {
        const issues = [{ path: "tables.tasks", message: "is not a parquet file" }]

        expect(problemDetail(problem({ issues }))).toBe("tables.tasks: is not a parquet file")
    })

    it("leaves out the path of a fault in the document itself", () => {
        expect(problemDetail(problem({ issues: [{ path: "", message: "is empty" }] }))).toBe("is empty")
    })

    it("stops at three faults and says how many it left out", () => {
        const issues = ["a", "b", "c", "d", "e"].map((path) => ({ path, message: "is wrong" }))

        expect(problemDetail(problem({ issues }))).toBe("a: is wrong; b: is wrong; c: is wrong; and 2 more")
    })

    it("falls back to the detail when no field is at fault, and to nothing when there is none", () => {
        expect(problemDetail(problem({ detail: "The store is full" }))).toBe("The store is full")
        expect(problemDetail(problem({}))).toBe("")
    })
})
