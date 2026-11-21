"use client"

import { ColorSwatch, Group, Text, UnstyledButton } from "@mantine/core"

export interface LegendEntry {
    name: string
    color: string
}

// Hovering or focusing a name isolates its line, since colour alone stops telling many runs apart.
export function SeriesLegend({
    entries,
    isolated,
    onIsolate,
}: {
    entries: LegendEntry[]
    isolated: string | undefined
    onIsolate: (name: string | undefined) => void
}) {
    return (
        <Group gap="md" wrap="wrap" justify="center" onMouseLeave={() => onIsolate(undefined)}>
            {entries.map((entry) => (
                <UnstyledButton
                    key={entry.name}
                    onMouseEnter={() => onIsolate(entry.name)}
                    onFocus={() => onIsolate(entry.name)}
                    onBlur={() => onIsolate(undefined)}
                    aria-label={`Show only ${entry.name}`}
                >
                    <Group gap={6} wrap="nowrap">
                        <ColorSwatch color={entry.color} size={10} withShadow={false} />
                        <Text size="xs" c={isolated === undefined || isolated === entry.name ? undefined : "dimmed"}>
                            {entry.name}
                        </Text>
                    </Group>
                </UnstyledButton>
            ))}
        </Group>
    )
}
