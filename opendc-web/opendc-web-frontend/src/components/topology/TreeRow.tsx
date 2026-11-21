"use client"

import type { TreeTarget } from "@/components/topology/tree"
import { Group, Text } from "@mantine/core"
import {
    IconAlertTriangle,
    IconBuilding,
    IconChevronDown,
    IconChevronRight,
    IconServer,
    IconSitemap,
    IconStack2,
} from "@tabler/icons-react"

const ICON_SIZE = 14

export function TreeRow({
    target,
    label,
    expanded,
    hasChildren,
    active,
    broken,
}: {
    target: TreeTarget
    label: string
    expanded: boolean
    hasChildren: boolean
    active: boolean
    broken: boolean
}) {
    return (
        <>
            {hasChildren ? (
                expanded ? (
                    <IconChevronDown size={ICON_SIZE} />
                ) : (
                    <IconChevronRight size={ICON_SIZE} />
                )
            ) : (
                <Group w={ICON_SIZE} />
            )}
            {iconOf(target)}
            <Text size="sm" fw={active ? 600 : 400} lineClamp={1}>
                {label}
            </Text>
            {broken && <IconAlertTriangle size={13} color="var(--mantine-color-red-6)" />}
        </>
    )
}

function iconOf(target: TreeTarget) {
    switch (target.kind) {
        case "topology":
            return <IconSitemap size={ICON_SIZE} />
        case "dataCenter":
            return <IconBuilding size={ICON_SIZE} />
        case "cluster":
            return <IconStack2 size={ICON_SIZE} />
        case "host":
            return <IconServer size={ICON_SIZE} />
    }
}
