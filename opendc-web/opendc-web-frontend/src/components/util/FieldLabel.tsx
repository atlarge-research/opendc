"use client"

import type { UnitLabel } from "@/lib/units"
import { Group, Text, Tooltip } from "@mantine/core"
import { IconInfoCircle } from "@tabler/icons-react"
import type { ReactNode } from "react"

// No size of its own: it inherits the surrounding label's, which differs between inspector and dialog.
export function FieldLabel({ label, help }: { label: string; help?: string }): ReactNode {
    if (!help) return label
    return (
        <Group gap={4} wrap="nowrap" component="span" display="inline-flex">
            {label}
            <Tooltip label={help} multiline w={260} withArrow position="top-start" openDelay={150}>
                <IconInfoCircle size={13} style={{ opacity: 0.6, flexShrink: 0 }} />
            </Tooltip>
        </Group>
    )
}

export function UnitAdornment({ unit }: { unit: UnitLabel }) {
    return (
        <Text size="xs" c="dimmed" pr={8}>
            {unit}
        </Text>
    )
}
