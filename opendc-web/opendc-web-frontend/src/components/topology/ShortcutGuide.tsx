"use client"

import { Box, Group, Kbd, Stack, Text } from "@mantine/core"
import { useOs } from "@mantine/hooks"
import type { ReactNode } from "react"

const TRIGGER_WIDTH = 122

export function ShortcutGuide() {
    const os = useOs()
    const mod = os === "macos" || os === "ios" ? "Cmd" : "Ctrl"

    return (
        <Stack gap="lg">
            <Section title="Mouse">
                <Row trigger={<Plain>Click a tile</Plain>} action="Adds a cluster there" />
                <Row trigger={<Plain>Drag a cluster</Plain>} action="Moves it, or swaps two" />
                <Row trigger={<Plain>Double click</Plain>} action="Opens its hosts" />
                <Row
                    trigger={
                        <Group gap={4} wrap="nowrap">
                            <Kbd size="xs">Shift</Kbd>
                            <Plain>click</Plain>
                        </Group>
                    }
                    action="Selects several"
                />
            </Section>

            <Section title="Keyboard">
                <Row trigger={<Keys parts={[mod, "D"]} />} action="Duplicate" />
                <Row trigger={<Keys parts={["Delete"]} />} action="Remove" />
                <Row trigger={<Keys parts={[mod, "Z"]} />} action="Undo" />
                <Row trigger={<Keys parts={[mod, "Shift", "Z"]} />} action="Redo" />
                <Row trigger={<Keys parts={[mod, "S"]} />} action="Save now" />
                <Row trigger={<Keys parts={["Esc"]} />} action="Clear selection" />
            </Section>
        </Stack>
    )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
    return (
        <Stack gap={7}>
            <Text size="10px" c="dimmed" tt="uppercase" fw={600} style={{ letterSpacing: "0.06em" }}>
                {title}
            </Text>
            {children}
        </Stack>
    )
}

function Row({ trigger, action }: { trigger: ReactNode; action: string }) {
    return (
        <Group gap="sm" wrap="nowrap" align="center">
            <Box w={TRIGGER_WIDTH} style={{ flexShrink: 0 }}>
                {trigger}
            </Box>
            <Text size="xs" c="dimmed">
                {action}
            </Text>
        </Group>
    )
}

function Keys({ parts }: { parts: string[] }) {
    return (
        <Group gap={4} wrap="nowrap">
            {parts.map((part) => (
                <Kbd key={part} size="xs">
                    {part}
                </Kbd>
            ))}
        </Group>
    )
}

function Plain({ children }: { children: ReactNode }) {
    return (
        <Text size="xs" c="dimmed">
            {children}
        </Text>
    )
}
