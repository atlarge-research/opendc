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
    const runs: { first: number; last: number }[] = []
    for (const index of sorted) {
        const run = runs.at(-1)
        if (run !== undefined && index === run.last + 1) {
            run.last = index
        } else {
            runs.push({ first: index, last: index })
        }
    }

    return runs.map(({ first, last }) => (first === last ? String(first) : `${first}-${last}`)).join(", ")
}
