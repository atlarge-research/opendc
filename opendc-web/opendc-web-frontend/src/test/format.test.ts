import { countOf, formatDuration, formatSimulationBudget } from "@/components/format"
import { describe, expect, it } from "vitest"

describe("formatDuration", () => {
    it("counts seconds, then minutes, then hours", () => {
        expect(formatDuration(45)).toBe("45s")
        expect(formatDuration(600)).toBe("10m")
        expect(formatDuration(2 * 3600)).toBe("2h")
        expect(formatDuration(3 * 3600 + 20 * 60)).toBe("3h 20m")
    })

    it("shows a value that rounds up to the next unit in that unit", () => {
        expect(formatDuration(59.7)).toBe("1m")
        expect(formatDuration(3599)).toBe("1h")
        expect(formatDuration(7199)).toBe("2h")
    })
})

describe("formatSimulationBudget", () => {
    it("names the largest unit the amount reaches", () => {
        expect(formatSimulationBudget(30)).toBe("30 simulation seconds")
        expect(formatSimulationBudget(5 * 60)).toBe("5 simulation minutes")
        expect(formatSimulationBudget(2 * 3600)).toBe("2 simulation hours")
    })

    it("shows a value that rounds up to the next unit in that unit", () => {
        expect(formatSimulationBudget(59.6)).toBe("1 simulation minute")
        expect(formatSimulationBudget(3599)).toBe("1 simulation hour")
    })
})

describe("countOf", () => {
    it("pluralises everything but one", () => {
        expect(countOf(0, "core")).toBe("0 cores")
        expect(countOf(1, "core")).toBe("1 core")
        expect(countOf(2, "core")).toBe("2 cores")
    })

    it("groups the digits of a large count", () => {
        expect(countOf(1234567, "unit")).toBe("1,234,567 units")
    })
})
