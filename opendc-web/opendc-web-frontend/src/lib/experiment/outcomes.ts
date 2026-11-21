import type { CarriedOutcome, ExitReason } from "@/lib/api/types"

export const EXIT_REASON_LABELS: Record<ExitReason, string> = {
    ok: "Exited cleanly",
    simulationError: "Simulation error",
    invalidSpec: "Invalid document",
    oom: "Out of memory",
    timeout: "Ran out of time",
    walltime: "Hit the platform's wall time",
    cancelled: "Cancelled",
    rejected: "Refused by the platform",
    unknown: "Lost by the platform",
}

export function outcomeLabel(outcome: CarriedOutcome): string {
    switch (outcome.type) {
        case "carried":
            return "Running"
        case "succeeded":
            return "Succeeded"
        case "failed":
            return EXIT_REASON_LABELS[outcome.reason]
        case "cancelled":
            return "Cancelled"
    }
}
