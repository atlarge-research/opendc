import { phaseBadge, phaseSince, scenarioRanges } from "@/lib/admin/phase"
import { describe, expect, it } from "vitest"

describe("phaseBadge", () => {
    it("flags a running execution past twice its estimate", () => {
        const onTime = phaseBadge({ type: "running", startedAt: "t", elapsedSeconds: 10, straggler: false })
        const overdue = phaseBadge({ type: "running", startedAt: "t", elapsedSeconds: 999, straggler: true })

        expect(onTime.label).toBe("Running")
        expect(overdue.label).toBe("Overdue")
        expect(overdue.color).not.toBe(onTime.color)
    })

    it("tells an ended execution's state apart from why its process exited", () => {
        const failedCleanly = phaseBadge({ type: "ended", state: "failed", settledAt: "t", reason: "ok", message: "" })

        expect(failedCleanly.label).toBe("Failed")
    })
})

describe("phaseSince", () => {
    it("reads the timestamp the phase guarantees, falling back to creation only while queued", () => {
        expect(phaseSince({ type: "queued" }, "created")).toBe("created")
        expect(phaseSince({ type: "submitted", submittedAt: "handed" }, "created")).toBe("handed")
        expect(
            phaseSince(
                { type: "ended", state: "succeeded", settledAt: "settled", reason: "ok", message: "" },
                "created",
            ),
        ).toBe("settled")
    })
})

describe("scenarioRanges", () => {
    it("collapses runs of consecutive scenarios and keeps gaps", () => {
        expect(scenarioRanges([9, 0, 1, 2, 3, 7, 10])).toBe("0-3, 7, 9-10")
    })

    it("counts a scenario carried twice once", () => {
        expect(scenarioRanges([4, 4, 5])).toBe("4-5")
    })

    it("reads nothing for no scenarios", () => {
        expect(scenarioRanges([])).toBe("")
    })
})
