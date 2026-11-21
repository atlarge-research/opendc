"use client"

import { ActionIcon, Group, Tabs, Tooltip } from "@mantine/core"
import { IconAlertTriangle, IconPlus } from "@tabler/icons-react"

/** One tab per data center, each showing that data center's floor. Tabs are addressed by index. */
export function DataCenterTabs({
    names,
    broken,
    active,
    onSelect,
    onAdd,
}: {
    names: string[]
    broken: number[]
    active: number
    onSelect: (dataCenter: number) => void
    onAdd?: () => void
}) {
    return (
        <Group gap={4} px="sm" wrap="nowrap">
            <Tabs
                value={String(active)}
                onChange={(value) => value !== null && onSelect(Number(value))}
                flex={1}
                miw={0}
            >
                <Tabs.List style={{ flexWrap: "nowrap", overflowX: "auto" }}>
                    {names.map((name, index) => (
                        <Tabs.Tab
                            // biome-ignore lint/suspicious/noArrayIndexKey: names repeat; floors line up by index
                            key={index}
                            value={String(index)}
                            rightSection={
                                broken.includes(index) ? (
                                    <IconAlertTriangle size={13} color="var(--mantine-color-red-6)" />
                                ) : undefined
                            }
                        >
                            {name}
                        </Tabs.Tab>
                    ))}
                </Tabs.List>
            </Tabs>
            {onAdd && (
                <Tooltip label="Add a data center">
                    <ActionIcon variant="subtle" color="gray" aria-label="Add a data center" onClick={onAdd}>
                        <IconPlus size={16} />
                    </ActionIcon>
                </Tooltip>
            )}
        </Group>
    )
}
