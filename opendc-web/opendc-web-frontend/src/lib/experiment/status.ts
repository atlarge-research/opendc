export const EXPERIMENT_STATES = ["draft", "queued", "running", "succeeded", "partial", "failed", "cancelled"] as const

export type ExperimentState = (typeof EXPERIMENT_STATES)[number]

export const SCENARIO_STATES = ["queued", "running", "succeeded", "failed", "cancelled"] as const

export type ScenarioExecutionState = (typeof SCENARIO_STATES)[number]

export interface ProgressReport {
    completedTasks: number
    totalTasks: number
}

const TERMINAL_SCENARIOS: ScenarioExecutionState[] = ["succeeded", "failed", "cancelled"]

const TERMINAL_EXPERIMENTS: ExperimentState[] = ["succeeded", "partial", "failed", "cancelled"]

export function isTerminalScenario(state: ScenarioExecutionState): boolean {
    return TERMINAL_SCENARIOS.includes(state)
}

export function isTerminalExperiment(state: ExperimentState): boolean {
    return TERMINAL_EXPERIMENTS.includes(state)
}

export function foldExperimentState(states: ScenarioExecutionState[]): ExperimentState {
    if (states.length === 0) return "draft"
    if (states.every((state) => state === "queued")) return "queued"
    if (states.some((state) => !isTerminalScenario(state))) return "running"
    if (states.every((state) => state === "succeeded")) return "succeeded"
    if (states.every((state) => state === "cancelled")) return "cancelled"
    if (states.every((state) => state === "failed")) return "failed"
    if (states.some((state) => state === "succeeded")) return "partial"
    return "failed"
}

export function progressFraction(report: ProgressReport): number {
    if (report.totalTasks <= 0) return 0
    return Math.min(1, Math.max(0, report.completedTasks / report.totalTasks))
}
