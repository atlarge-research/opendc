import { problemOf } from "@/lib/api/client"
import type { ApiProblem, DocumentIssue } from "@/lib/api/types"
import { notifications } from "@mantine/notifications"

const SHOWN_ISSUES = 3

export function notifySaved(what: string): void {
    notifications.show({ title: `${what} saved`, message: "", color: "opendc" })
}

export function notifyProblem(error: unknown): void {
    const problem = problemOf(error)
    notifications.show({ title: problem.title, message: problemDetail(problem), color: "red" })
}

/** What a notification says under a problem's title: the first few faulted fields with their reasons. */
export function problemDetail({ issues, detail }: ApiProblem): string {
    if (issues.length === 0) return detail ?? ""

    const shown = issues.slice(0, SHOWN_ISSUES).map(describeIssue)
    const hidden = issues.length - shown.length
    return hidden > 0 ? `${shown.join("; ")}; and ${hidden} more` : shown.join("; ")
}

function describeIssue({ path, message }: DocumentIssue): string {
    return path === "" ? message : `${path}: ${message}`
}

export function notifyComingSoon(feature: string): void {
    notifications.show({
        title: `${feature} is not ready yet`,
        message: "This is a preview build and is not wired up to the backend yet.",
        color: "opendc",
    })
}
