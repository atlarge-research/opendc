"use client"

import type { ExperimentState, ScenarioExecutionState } from "@/lib/experiment/status"
import { Badge } from "@mantine/core"

const EXPERIMENT_COLORS: Record<ExperimentState, string> = {
    draft: "gray",
    queued: "blue",
    running: "opendc",
    succeeded: "green",
    partial: "yellow",
    failed: "red",
    cancelled: "gray",
}

const SCENARIO_COLORS: Record<ScenarioExecutionState, string> = {
    queued: "blue",
    running: "opendc",
    succeeded: "green",
    failed: "red",
    cancelled: "gray",
}

export function ExperimentStateBadge({ state }: { state: ExperimentState }) {
    return (
        <Badge color={EXPERIMENT_COLORS[state]} variant="light" tt="capitalize">
            {state}
        </Badge>
    )
}

export function ScenarioStateBadge({ state }: { state: ScenarioExecutionState }) {
    return (
        <Badge color={SCENARIO_COLORS[state]} variant="light" size="sm" tt="capitalize">
            {state}
        </Badge>
    )
}
