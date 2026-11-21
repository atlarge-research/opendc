import { problemOf } from "@/lib/api/client"
import { notifications } from "@mantine/notifications"

export function notifySaved(what: string): void {
    notifications.show({ title: `${what} saved`, message: "", color: "opendc" })
}

export function notifyProblem(error: unknown): void {
    const problem = problemOf(error)
    const detail = problem.issues.length > 0 ? problem.issues.map((issue) => issue.path).join(", ") : problem.detail
    notifications.show({ title: problem.title, message: detail ?? "", color: "red" })
}

export function notifyComingSoon(feature: string): void {
    notifications.show({
        title: `${feature} is not ready yet`,
        message: "This is a preview build and is not wired up to the backend yet.",
        color: "opendc",
    })
}
