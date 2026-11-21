import type { ExecutionPhase, ExecutionState } from "@/lib/api/admin"

export interface PhaseBadge {
    label: string
    color: string
}

export const STATE_BADGES: Record<ExecutionState, PhaseBadge> = {
    queued: { label: "Queued", color: "blue" },
    submitted: { label: "Submitted", color: "cyan" },
    running: { label: "Running", color: "opendc" },
    succeeded: { label: "Succeeded", color: "green" },
    failed: { label: "Failed", color: "red" },
    cancelled: { label: "Cancelled", color: "gray" },
}

const OVERDUE_BADGE: PhaseBadge = { label: "Overdue", color: "orange" }

/** How an execution's phase reads in a list: a running one past twice its estimate stands out. */
export function phaseBadge(phase: ExecutionPhase): PhaseBadge {
    switch (phase.type) {
        case "queued":
        case "submitted":
            return STATE_BADGES[phase.type]
        case "running":
            return phase.straggler ? OVERDUE_BADGE : STATE_BADGES.running
        case "ended":
            return STATE_BADGES[phase.state]
    }
}

/** When the execution last changed phase. */
export function phaseSince(phase: ExecutionPhase, createdAt: string): string {
    switch (phase.type) {
        case "queued":
            return createdAt
        case "submitted":
            return phase.submittedAt
        case "running":
            return phase.startedAt
        case "ended":
            return phase.settledAt
    }
}

/** Scenario indices as a compact range list: 0-3, 7, 9-10. */
export function scenarioRanges(indices: readonly number[]): string {
    const sorted = [...new Set(indices)].sort((left, right) => left - right)
    const ranges: string[] = []
    let start = sorted[0]
    let previous = start
    for (const index of sorted.slice(1)) {
        if (previous !== undefined && index === previous + 1) {
            previous = index
            continue
        }
        if (start !== undefined) ranges.push(rangeOf(start, previous ?? start))
        start = index
        previous = index
    }
    if (start !== undefined) ranges.push(rangeOf(start, previous ?? start))
    return ranges.join(", ")
}

function rangeOf(start: number, end: number): string {
    return start === end ? String(start) : `${start}-${end}`
}
