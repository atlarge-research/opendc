"use client"

import { Anchor, Breadcrumbs, Group, Text } from "@mantine/core"
import { type Icon, IconFlask, IconFolder, IconFolders, IconSitemap } from "@tabler/icons-react"
import Link from "next/link"
import type { ReactNode } from "react"

export type EntityKind = "projects" | "project" | "topology" | "experiment"

const ENTITY_ICONS: Record<EntityKind, Icon> = {
    projects: IconFolders,
    project: IconFolder,
    topology: IconSitemap,
    experiment: IconFlask,
}

export interface Crumb {
    kind: EntityKind
    label: string
    href?: string
}

export function EntityBreadcrumbs({ crumbs, trailing }: { crumbs: Crumb[]; trailing?: ReactNode }) {
    return (
        <Group gap={6} wrap="nowrap">
            <Breadcrumbs separator="/">
                {crumbs.map((crumb) => (
                    <Crumb key={`${crumb.kind}-${crumb.label}`} crumb={crumb} />
                ))}
            </Breadcrumbs>
            {trailing}
        </Group>
    )
}

function Crumb({ crumb }: { crumb: Crumb }) {
    const EntityIcon = ENTITY_ICONS[crumb.kind]
    const icon = <EntityIcon size={14} style={{ flexShrink: 0 }} />

    if (crumb.href === undefined) {
        return (
            <Group gap={5} wrap="nowrap">
                {icon}
                <Text size="sm" fw={500}>
                    {crumb.label}
                </Text>
            </Group>
        )
    }

    return (
        <Anchor component={Link} href={crumb.href} size="sm">
            <Group gap={5} wrap="nowrap">
                {icon}
                {crumb.label}
            </Group>
        </Anchor>
    )
}
