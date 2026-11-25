"use client"

import { AXIS_LABELS, PRIMARY_AXES, RUNS_LABEL, axisEntryLabels } from "@/components/experiment/axisLabels"
import { formatCount } from "@/components/format"
import type { Id } from "@/lib/api/types"
import {
    AXIS_ORDER,
    type AxisKey,
    type ExperimentSpec,
    experimentAxes,
    experimentRuns,
    scenarioCount,
} from "@/lib/experiment/spec"
import { Button, Grid, Paper, Stack, Table, Text, Title } from "@mantine/core"
import { IconPlayerPlay, IconSitemap, IconUpload } from "@tabler/icons-react"
import Link from "next/link"
import type { ReactNode } from "react"

const TIMES = <>&times;</>
const EQUALS = "="

const ALWAYS_SHOWN = new Set<AxisKey>(PRIMARY_AXES)

export function ExperimentHelp({ spec, projectId }: Readonly<{ spec: ExperimentSpec; projectId: Id }>) {
    return (
        <Grid gutter="md">
            <Grid.Col span={{ base: 12, lg: 8 }}>
                <Stack gap="md">
                    <Section title="Experiment cartesian: building scenarios">
                        <Text size="sm">
                            An experiment consists of multiple scenarios that are being simulated. Every choice below is
                            a list, and the experiment runs one scenario for each way of taking a single entry from each
                            list. The lists multiply, therefore adding a new topology doubles the number of scenarios
                            considered.
                        </Text>
                        <Expansion spec={spec} />
                        <Text size="sm" c="dimmed">
                            The table on the Overview tab names every scenario this adds up to, so you can read the
                            whole set before spending anything on it.
                        </Text>
                    </Section>
                    <Section title="Executing the experiment">
                        <Text size="sm">
                            Once you are done configuring an experiment, click the{" "}
                            <Text span inherit c="opendc" fw={600}>
                                <IconPlayerPlay size={16} style={{ verticalAlign: "middle", marginRight: 2 }} />
                                Run experiment
                            </Text>{" "}
                            button above to execute it!
                        </Text>
                        <Text size="sm">
                            Running the experiment queues every scenario at once and reserves simulation budget for the
                            estimate shown beside the editor. A submitted experiment is frozen: its configuration can no
                            longer change, which is what makes its numbers worth quoting. To try a variation, clone it
                            to a new draft and edit the copy.
                        </Text>
                        <Text size="sm">
                            Results appear under the Results tab as scenarios finish, and Export configuration downloads
                            the whole setup as JSON to keep alongside them.
                        </Text>
                    </Section>
                </Stack>
            </Grid.Col>

            <Grid.Col span={{ base: 12, lg: 4 }}>
                <Stack gap="md">
                    <LinkCard
                        title="Bring your own traces"
                        href="/traces"
                        label="Open traces"
                        icon={<IconUpload size={16} />}
                    >
                        <Text size="sm">
                            Workloads, carbon intensity and failure records all arrive as traces. Upload one parquet
                            file per table the kind asks for, a workload taking tasks and fragments, and the trace joins
                            the matching list here.
                        </Text>
                        <Text size="sm">
                            Traces are not bound to this one project, but instead are accessible across all of your
                            projects. You may share traces you uploaded with other users.
                        </Text>
                    </LinkCard>
                    <LinkCard
                        title="Building a topology"
                        href={`/project?id=${projectId}`}
                        label="Open project"
                        icon={<IconSitemap size={16} />}
                    >
                        <Text size="sm">
                            Topologies are drawn in the topology editor and saved to the project, so several experiments
                            can measure the same datacenter.
                        </Text>
                    </LinkCard>
                </Stack>
            </Grid.Col>
        </Grid>
    )
}

function Section({ title, children }: Readonly<{ title: string; children: ReactNode }>) {
    return (
        <Paper withBorder radius="md" p="md">
            <Stack gap="sm">
                <Title order={4}>{title}</Title>
                {children}
            </Stack>
        </Paper>
    )
}

function LinkCard({
    title,
    href,
    label,
    icon,
    children,
}: Readonly<{ title: string; href: string; label: string; icon: ReactNode; children: ReactNode }>) {
    return (
        <Paper withBorder radius="md" p="md">
            <Stack gap="xs">
                <Title order={5}>{title}</Title>
                {children}
                <Button component={Link} href={href} variant="light" leftSection={icon} mt={4} fullWidth>
                    {label}
                </Button>
            </Stack>
        </Paper>
    )
}

// Worked out on the draft's own axes rather than an invented example.
function Expansion({ spec }: Readonly<{ spec: ExperimentSpec }>) {
    const axes = experimentAxes(spec)
    const shown = [...AXIS_ORDER].reverse().filter((key) => ALWAYS_SHOWN.has(key) || axes[key].length > 1)
    const scenarios = scenarioCount(spec)
    const runs = experimentRuns(spec)

    return (
        <Stack gap="xs">
            <Table verticalSpacing="xs" horizontalSpacing="sm">
                <Table.Tbody>
                    {shown.map((key, index) => (
                        <Row
                            key={key}
                            operator={index === 0 ? null : TIMES}
                            label={AXIS_LABELS[key]}
                            detail={axisEntryLabels(axes, key).join(", ")}
                            value={axes[key].length}
                        />
                    ))}
                    <Row operator={EQUALS} label="Scenarios" value={scenarios} strong />
                    <Row operator={TIMES} label={RUNS_LABEL} value={runs} />
                    <Row operator={EQUALS} label="Total simulation runs" value={scenarios * runs} strong />
                </Table.Tbody>
            </Table>
            {scenarios === 0 && (
                <Text size="sm" c="dimmed">
                    A list left empty takes every combination with it, which is why this comes to nothing. Pick at least
                    one entry on each.
                </Text>
            )}
        </Stack>
    )
}

function Row({
    operator,
    label,
    detail = "",
    value,
    strong = false,
}: Readonly<{ operator: ReactNode; label: string; detail?: string; value: number; strong?: boolean }>) {
    const weight = strong ? 600 : 400

    return (
        <Table.Tr>
            <Table.Td w={24}>
                <Text size="sm" c="dimmed">
                    {operator}
                </Text>
            </Table.Td>
            <Table.Td>
                <Text size="sm" fw={weight}>
                    {label}
                </Text>
            </Table.Td>
            <Table.Td>
                <Text size="xs" c="dimmed" lineClamp={1}>
                    {detail}
                </Text>
            </Table.Td>
            <Table.Td w={72} ta="right">
                <Text size="sm" fw={weight}>
                    {formatCount(value)}
                </Text>
            </Table.Td>
        </Table.Tr>
    )
}
