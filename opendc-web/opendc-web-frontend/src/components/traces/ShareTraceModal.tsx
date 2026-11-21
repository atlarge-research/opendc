"use client"

import { LineGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { notifyProblem } from "@/components/util/feedback"
import { useRevokeShare, useShareTrace, useShares } from "@/lib/api/traces"
import type { Trace } from "@/lib/api/types"
import { ActionIcon, Button, Group, Stack, Text, TextInput } from "@mantine/core"
import { modals } from "@mantine/modals"
import { IconX } from "@tabler/icons-react"
import { useState } from "react"

const MODAL_ID = "share-trace"

export function openShareTrace(trace: Trace): void {
    modals.open({
        modalId: MODAL_ID,
        title: `Share ${trace.slug}`,
        children: <ShareTraceForm trace={trace} />,
    })
}

function ShareTraceForm({ trace }: { trace: Trace }) {
    const [handle, setHandle] = useState("")
    const shares = useShares(trace.id, true)
    const share = useShareTrace(trace.id)
    const revoke = useRevokeShare(trace.id)
    const trimmed = handle.trim()

    const submit = () => {
        if (trimmed === "") return
        share.mutate(trimmed, { onSuccess: () => setHandle(""), onError: notifyProblem })
    }

    return (
        <Stack>
            <Text size="sm" c="dimmed">
                Whoever you share this with can use it in their own experiments. Taking a share back does not disturb an
                experiment that already ran.
            </Text>
            <Group align="flex-end" gap="xs">
                <TextInput
                    flex={1}
                    label="Account handle"
                    placeholder="alice"
                    value={handle}
                    data-autofocus
                    onChange={(event) => setHandle(event.currentTarget.value)}
                    onKeyDown={(event) => {
                        if (event.key === "Enter") submit()
                    }}
                />
                <Button onClick={submit} disabled={trimmed === ""} loading={share.isPending}>
                    Share
                </Button>
            </Group>
            <QueryState query={shares} ghost={<LineGhost />}>
                {(loaded) =>
                    loaded.length === 0 ? (
                        <Text size="sm" c="dimmed">
                            Nobody else has this yet.
                        </Text>
                    ) : (
                        <Stack gap={4}>
                            {loaded.map((entry) => (
                                <Group key={entry.handle} justify="space-between" wrap="nowrap">
                                    <Text size="sm">
                                        {entry.displayName} ({entry.handle})
                                    </Text>
                                    <ActionIcon
                                        variant="subtle"
                                        color="red"
                                        aria-label={`Stop sharing with ${entry.handle}`}
                                        onClick={() => revoke.mutate(entry.handle, { onError: notifyProblem })}
                                    >
                                        <IconX size={16} />
                                    </ActionIcon>
                                </Group>
                            ))}
                        </Stack>
                    )
                }
            </QueryState>
        </Stack>
    )
}
