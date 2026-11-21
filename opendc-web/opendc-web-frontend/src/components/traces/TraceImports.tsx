"use client"

import { formatUpdatedAt } from "@/components/format"
import { notifyProblem } from "@/components/util/feedback"
import { useDismissImport, useTraceImports } from "@/lib/api/traces"
import type { TraceImport } from "@/lib/api/types"
import { ActionIcon, Group, Loader, Paper, Stack, Text, ThemeIcon, Tooltip } from "@mantine/core"
import { IconAlertTriangle, IconCheck, IconX } from "@tabler/icons-react"

/**
 * Imports the reader started, until they dismiss them. A succeeded one is already in the library
 * below; it stays listed only so the reader sees that it finished.
 */
export function TraceImports() {
    const imports = useTraceImports()
    const entries = imports.data ?? []
    if (entries.length === 0) return undefined

    return (
        <Paper withBorder radius="md" p="sm">
            <Stack gap="xs">
                {entries.map((entry) => (
                    <ImportRow key={entry.id} entry={entry} />
                ))}
            </Stack>
        </Paper>
    )
}

function ImportRow({ entry }: Readonly<{ entry: TraceImport }>) {
    const dismiss = useDismissImport()
    const { progress } = entry

    return (
        <Group justify="space-between" wrap="nowrap">
            <Group gap="sm" wrap="nowrap">
                <ProgressIcon entry={entry} />
                <Stack gap={0}>
                    <Text size="sm" fw={500}>
                        {entry.slug}
                    </Text>
                    <Text size="xs" c={progress.type === "failed" ? "red" : "dimmed"}>
                        {describe(entry)}
                    </Text>
                </Stack>
            </Group>
            {progress.type !== "running" && (
                <Tooltip label="Dismiss" withArrow>
                    <ActionIcon
                        variant="subtle"
                        color="gray"
                        aria-label={`Dismiss the import of ${entry.slug}`}
                        loading={dismiss.isPending}
                        onClick={() => dismiss.mutate(entry.id, { onError: notifyProblem })}
                    >
                        <IconX size={16} />
                    </ActionIcon>
                </Tooltip>
            )}
        </Group>
    )
}

function ProgressIcon({ entry }: Readonly<{ entry: TraceImport }>) {
    switch (entry.progress.type) {
        case "running":
            return <Loader size={18} />
        case "succeeded":
            return (
                <ThemeIcon size={20} radius="xl" color="green" variant="light">
                    <IconCheck size={14} />
                </ThemeIcon>
            )
        case "failed":
            return (
                <ThemeIcon size={20} radius="xl" color="red" variant="light">
                    <IconAlertTriangle size={14} />
                </ThemeIcon>
            )
    }
}

function describe(entry: TraceImport): string {
    switch (entry.progress.type) {
        case "running":
            return `Importing since ${formatUpdatedAt(entry.progress.since)}`
        case "succeeded":
            return `Imported ${formatUpdatedAt(entry.progress.at)}`
        case "failed":
            return entry.progress.reason
    }
}
