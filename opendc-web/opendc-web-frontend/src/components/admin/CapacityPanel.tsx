"use client"

import { formatDuration, formatMemory, formatPercent } from "@/components/format"
import { PanelGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { type PlatformCapacity, useAdminCapacity } from "@/lib/api/admin"
import { Paper, Progress, SimpleGrid, Stack, Text } from "@mantine/core"

export function CapacityPanel() {
    const capacity = useAdminCapacity()
    return (
        <QueryState query={capacity} ghost={<PanelGhost />}>
            {(loaded) => <CapacityView capacity={loaded} />}
        </QueryState>
    )
}

function CapacityView({ capacity }: Readonly<{ capacity: PlatformCapacity }>) {
    const timeCap =
        capacity.slotTimeCap.type === "limited" ? formatDuration(capacity.slotTimeCap.seconds) : "no time limit"
    return (
        <Stack gap="md">
            <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <Usage
                    label="Cores"
                    used={capacity.allocatedCores}
                    total={capacity.totalCores}
                    format={(value) => String(value)}
                />
                <Usage
                    label="Memory"
                    used={capacity.allocatedMemoryMb}
                    total={capacity.totalMemoryMb}
                    format={formatMemory}
                />
            </SimpleGrid>
            <Text size="sm" c="dimmed">
                The {capacity.dispatcher} dispatcher gives one execution at most {capacity.slotCores} cores and{" "}
                {formatMemory(capacity.slotMemoryMb)}, with {timeCap}.
            </Text>
        </Stack>
    )
}

function Usage({
    label,
    used,
    total,
    format,
}: Readonly<{ label: string; used: number; total: number; format: (value: number) => string }>) {
    const fraction = total > 0 ? used / total : 0
    return (
        <Paper withBorder radius="md" p="md">
            <Stack gap="xs">
                <Text size="sm" fw={500}>
                    {label}
                </Text>
                <Progress value={fraction * 100} aria-label={`${label} in use`} />
                <Text size="sm" c="dimmed">
                    {format(used)} of {format(total)} in use ({formatPercent(fraction)})
                </Text>
            </Stack>
        </Paper>
    )
}
