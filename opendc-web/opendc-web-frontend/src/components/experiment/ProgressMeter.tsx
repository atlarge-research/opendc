"use client"

import { formatPercent } from "@/components/format"
import { Group, Progress, type ProgressProps, Text } from "@mantine/core"

export function ProgressMeter({
    fraction,
    failed,
    width,
    size,
    label,
}: {
    fraction: number
    failed: boolean
    width: number
    size?: ProgressProps["size"]
    label: string
}) {
    return (
        <Group gap="xs" wrap="nowrap">
            <Progress
                value={fraction * 100}
                w={width}
                size={size}
                color={failed ? "red" : "opendc"}
                aria-label={label}
            />
            <Text size="xs" c="dimmed">
                {formatPercent(fraction)}
            </Text>
        </Group>
    )
}
