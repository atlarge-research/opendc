import { budgetColor, budgetPercent, formatBudgetUsage, formatResetsAt } from "@/components/user/accountFormat"
import type { BudgetWindow, SimulationCap } from "@/lib/api/types"
import { describe, expect, it } from "vitest"

const limited = (seconds: number): SimulationCap => ({ type: "limited", seconds })
const unlimited: SimulationCap = { type: "unlimited" }

function budget(overrides: Partial<BudgetWindow> = {}): BudgetWindow {
    return {
        period: "week",
        usedSeconds: 1800,
        reservedSeconds: 0,
        cap: limited(3600),
        resetsAt: "2026-07-25T18:30:00Z",
        ...overrides,
    }
}

describe("budgetPercent", () => {
    it("reports an uncapped window as unconsumed rather than as full", () => {
        expect(budgetPercent(budget({ cap: unlimited, usedSeconds: 999_999 }))).toBe(0)
    })

    it("treats a zero cap as fully consumed instead of dividing by zero", () => {
        expect(budgetPercent(budget({ cap: limited(0), usedSeconds: 0 }))).toBe(100)
    })

    it("clamps a cap that has been overspent", () => {
        expect(budgetPercent(budget({ cap: limited(100), usedSeconds: 500 }))).toBe(100)
    })

    it("rounds to the nearest whole percent", () => {
        expect(budgetPercent(budget({ cap: limited(3600), usedSeconds: 1000 }))).toBe(28)
    })
})

describe("budgetColor", () => {
    it("escalates exactly at the warning and danger thresholds", () => {
        expect(budgetColor(74)).toBe("opendc")
        expect(budgetColor(75)).toBe("yellow")
        expect(budgetColor(89)).toBe("yellow")
        expect(budgetColor(90)).toBe("red")
    })
})

describe("formatBudgetUsage", () => {
    it("names an uncapped allowance in words rather than as a symbol", () => {
        expect(formatBudgetUsage(budget({ cap: unlimited, usedSeconds: 3480 }))).toBe("58 / unlimited simulation min")
    })

    it("rounds partial minutes up so consumed time is never understated", () => {
        expect(formatBudgetUsage(budget({ usedSeconds: 61, cap: limited(119) }))).toBe("2 / 2 simulation min")
    })
})

describe("formatResetsAt", () => {
    it("omits the weekday for a session budget but keeps it for a longer period", () => {
        expect(formatResetsAt(budget({ period: "session" }))).toMatch(/^Resets at \d{2}:\d{2}$/)
        expect(formatResetsAt(budget({ period: "week" }))).toMatch(/^Resets \w{3} \d{2}:\d{2}$/)
    })
})
