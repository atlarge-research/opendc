"use client"

import { PanelGhost } from "@/components/util/Ghost"
import { useExecutionLog } from "@/lib/api/admin"
import { Code, ScrollArea, Stack, Text } from "@mantine/core"

/** The launcher's log, which the platform hands over only once the execution has ended. */
export function ExecutionLog({ id, available }: Readonly<{ id: string; available: boolean }>) {
    const log = useExecutionLog(id, available)

    if (!available) {
        return (
            <Text size="sm" c="dimmed">
                The log is collected when the execution ends.
            </Text>
        )
    }
    if (log.isPending) return <PanelGhost height={240} />
    if (log.isError) {
        return (
            <Text size="sm" c="dimmed">
                No log was collected for this execution.
            </Text>
        )
    }
    return (
        <Stack gap={4}>
            <Text size="sm" fw={500}>
                Launcher log
            </Text>
            <ScrollArea.Autosize mah={400} type="auto">
                <Code block fz="xs">
                    {log.data}
                </Code>
            </ScrollArea.Autosize>
        </Stack>
    )
}
