import type { BudgetWindow } from "@/lib/api/types"
import dayjs from "dayjs"

const euro = new Intl.NumberFormat("en-IE", { style: "currency", currency: "EUR" })

const BUDGET_DANGER_PERCENT = 90

const BUDGET_WARNING_PERCENT = 75

export function budgetPercent({ usedSeconds, cap }: BudgetWindow): number {
    // An uncapped window has no proportion to show, so the bar stays empty rather than full.
    if (cap.type === "unlimited") return 0
    if (cap.seconds <= 0) return 100
    return Math.min(100, Math.round((usedSeconds / cap.seconds) * 100))
}

export function budgetColor(percent: number): string {
    if (percent >= BUDGET_DANGER_PERCENT) return "red"
    if (percent >= BUDGET_WARNING_PERCENT) return "yellow"
    return "opendc"
}

export function formatBudgetUsage({ usedSeconds, cap }: BudgetWindow): string {
    const allowance = cap.type === "unlimited" ? "unlimited" : String(simulationMinutes(cap.seconds))
    return `${simulationMinutes(usedSeconds)} / ${allowance} simulation min`
}

function simulationMinutes(seconds: number): number {
    return Math.ceil(seconds / 60)
}

export function formatResetsAt({ period, resetsAt }: BudgetWindow): string {
    const pattern = period === "session" ? "[at] HH:mm" : "ddd HH:mm"
    return `Resets ${dayjs(resetsAt).format(pattern)}`
}

export function formatBillingDate(date: string): string {
    return dayjs(date).format("MMM D, YYYY")
}

export function formatAmount(amountEur: number): string {
    return euro.format(amountEur)
}
