"use client"

import { AxisSelect } from "@/components/experiment/AxisSelect"
import { CheckpointAxis } from "@/components/experiment/CheckpointAxis"
import { ExportAxis } from "@/components/experiment/ExportAxis"
import { FailureBudgetAxis } from "@/components/experiment/FailureBudgetAxis"
import { AXIS_HELP, AXIS_LABELS, RUNS_HELP, RUNS_LABEL, axisEntryLabels } from "@/components/experiment/axisLabels"
import { bindFailureModels, bindSchedulers, bindTopologies, bindWorkloads } from "@/components/experiment/draftAxes"
import { formatCount, formatSimulationBudget } from "@/components/format"
import { FieldLabel } from "@/components/util/FieldLabel"
import { notifyProblem } from "@/components/util/feedback"
import { useCatalog } from "@/lib/api/catalogs"
import { useSaveExperimentDraft } from "@/lib/api/experiments"
import { useTopologies } from "@/lib/api/topologies"
import { useTraceOptions } from "@/lib/api/traces"
import type { Experiment } from "@/lib/api/types"
import {
    DEFAULT_RUNS,
    type ExperimentSpec,
    allocationPolicyAxis,
    experimentAxes,
    experimentRuns,
    failureModelAxis,
    scenarioCount,
} from "@/lib/experiment/spec"
import { usePermission } from "@/lib/project/permissions"
import { Accordion, Divider, Fieldset, Group, NumberInput, Paper, Stack, Text } from "@mantine/core"
import { useDebouncedCallback } from "@mantine/hooks"
import { useState } from "react"

const SAVE_DELAY_MS = 600
const MAX_RUNS = 32

export function DraftEditor({ experiment }: { experiment: Experiment }) {
    const [spec, setSpec] = useState(experiment.spec)
    const save = useSaveExperimentDraft(experiment.id)
    const templates = useTopologies(experiment.projectId)
    const traces = useTraceOptions("workload")
    const schedulers = useCatalog("schedulers")
    const prefabs = useCatalog("failure-prefabs")
    const canEdit = usePermission(experiment.projectId, "edit")

    const persist = useDebouncedCallback(
        (next: ExperimentSpec) => save.mutate({ name: experiment.name, spec: next }, { onError: notifyProblem }),
        SAVE_DELAY_MS,
    )

    const edit = (change: (current: ExperimentSpec) => ExperimentSpec) =>
        setSpec((current) => {
            const next = change(current)
            persist(next)
            return next
        })

    const axes = experimentAxes(spec)
    const topologies = bindTopologies(spec.topologies, templates.data ?? [])
    const workloads = bindWorkloads(spec.workloads, traces.data ?? [])
    const allocation = bindSchedulers(allocationPolicyAxis(spec), schedulers.data ?? [])
    const failures = bindFailureModels(failureModelAxis(spec), prefabs.data ?? [])

    return (
        <Paper withBorder radius="md" p="md">
            <Fieldset variant="unstyled" disabled={!canEdit}>
                <Stack gap="md">
                    <AxisSelect
                        label={AXIS_LABELS.topologies}
                        help={AXIS_HELP.topologies}
                        choices={topologies.choices}
                        value={topologies.selected}
                        onChange={(values) =>
                            edit((current) => ({ ...current, topologies: topologies.rebuild(values) }))
                        }
                    />
                    <AxisSelect
                        label={AXIS_LABELS.workloads}
                        help={AXIS_HELP.workloads}
                        choices={workloads.choices}
                        value={workloads.selected}
                        onChange={(values) => edit((current) => ({ ...current, workloads: workloads.rebuild(values) }))}
                    />
                    <AxisSelect
                        label={AXIS_LABELS.allocationPolicies}
                        help={AXIS_HELP.allocationPolicies}
                        choices={allocation.choices}
                        value={allocation.selected}
                        onChange={(values) =>
                            edit((current) => ({ ...current, allocationPolicies: allocation.rebuild(values) }))
                        }
                    />
                    <AxisSelect
                        label={AXIS_LABELS.failureModels}
                        help={AXIS_HELP.failureModels}
                        choices={failures.choices}
                        value={failures.selected}
                        onChange={(values) =>
                            edit((current) => ({ ...current, failureModels: failures.rebuild(values) }))
                        }
                    />

                    <FailureBudgetAxis
                        entries={axes.maxNumFailures}
                        // An empty axis expands to no scenarios, so clearing it drops the key instead.
                        onChange={(next) =>
                            edit((current) => ({ ...current, maxNumFailures: next.length === 0 ? undefined : next }))
                        }
                    />

                    <ExportAxis
                        entries={axes.exportModels}
                        onChange={(next) =>
                            edit((current) => ({ ...current, exportModels: next.length === 0 ? undefined : next }))
                        }
                    />

                    <NumberInput
                        label={<FieldLabel label={RUNS_LABEL} help={RUNS_HELP} />}
                        min={1}
                        max={MAX_RUNS}
                        value={experimentRuns(spec)}
                        onChange={(value) =>
                            edit((current) => ({ ...current, runs: typeof value === "number" ? value : DEFAULT_RUNS }))
                        }
                    />

                    {/* The control summarises the axis so it stays readable while folded. */}
                    <Accordion variant="contained" chevronPosition="left">
                        <Accordion.Item value="checkpointModels">
                            <Accordion.Control>
                                <Group justify="space-between" wrap="nowrap" gap="sm">
                                    <Text size="sm">{AXIS_LABELS.checkpointModels}</Text>
                                    <Text size="xs" c="dimmed" lineClamp={1}>
                                        {axisEntryLabels(axes, "checkpointModels").join(", ")}
                                    </Text>
                                </Group>
                            </Accordion.Control>
                            <Accordion.Panel>
                                <CheckpointAxis
                                    entries={axes.checkpointModels}
                                    onChange={(next) => edit((current) => ({ ...current, checkpointModels: next }))}
                                />
                            </Accordion.Panel>
                        </Accordion.Item>
                    </Accordion>

                    <Divider my={4} />

                    <Group justify="space-between" wrap="nowrap" gap="sm">
                        <Text size="sm" c="dimmed">
                            {formatCount(scenarioCount(spec))} scenarios, about{" "}
                            {formatSimulationBudget(experiment.estimate.estimatedBudgetSeconds)}
                        </Text>
                        <Text size="xs" c="dimmed">
                            {save.isPending ? "Saving" : "Saved"}
                        </Text>
                    </Group>
                </Stack>
            </Fieldset>
        </Paper>
    )
}
