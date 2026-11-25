"use client"

import { ActionIcon, Alert, Code, CopyButton, Group, Stack, Text, Tooltip } from "@mantine/core"
import { IconCheck, IconCopy, IconKey } from "@tabler/icons-react"

/** A token's secret, shown the one time it exists outside the person's hands. */
export function TokenSecret({ secret }: Readonly<{ secret: string }>) {
    return (
        <Alert color="yellow" icon={<IconKey size={16} />} title="Copy this token now">
            <Stack gap="xs">
                <Text size="sm">It is not shown again. Use it as OPENDC_TOKEN, or with --token, in the CLI.</Text>
                <Group gap="xs" wrap="nowrap">
                    <Code style={{ overflowWrap: "anywhere" }}>{secret}</Code>
                    <CopyButton value={secret}>
                        {({ copied, copy }) => (
                            <Tooltip label={copied ? "Copied" : "Copy"}>
                                <ActionIcon variant="subtle" onClick={copy} aria-label="Copy the token">
                                    {copied ? <IconCheck size={16} /> : <IconCopy size={16} />}
                                </ActionIcon>
                            </Tooltip>
                        )}
                    </CopyButton>
                </Group>
            </Stack>
        </Alert>
    )
}
