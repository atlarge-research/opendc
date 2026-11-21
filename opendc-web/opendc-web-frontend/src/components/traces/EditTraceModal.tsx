"use client"

import { TRACE_NAME_HELP, traceNameProblem } from "@/components/traces/traceName"
import { FieldLabel } from "@/components/util/FieldLabel"
import { notifyProblem, notifySaved } from "@/components/util/feedback"
import { useEditTrace } from "@/lib/api/traces"
import type { Trace } from "@/lib/api/types"
import { Button, Group, Stack, TextInput } from "@mantine/core"
import { modals } from "@mantine/modals"
import { useState } from "react"

const MODAL_ID = "edit-trace"

export function openEditTrace(trace: Trace): void {
    modals.open({ modalId: MODAL_ID, title: `Edit ${trace.slug}`, children: <EditTraceForm trace={trace} /> })
}

function EditTraceForm({ trace }: { trace: Trace }) {
    const edit = useEditTrace()
    // Only the part after the handle is the owner's to change.
    const [name, setName] = useState(trace.slug.split("/").pop() ?? "")
    const [description, setDescription] = useState(trace.description ?? "")
    const problem = traceNameProblem(name)

    // The server renames only when the name changed, so a referenced trace's description stays editable.
    const save = () => {
        if (problem !== undefined) return
        edit.mutate(
            { id: trace.id, name: name.trim(), description },
            {
                onSuccess: () => {
                    notifySaved("Trace")
                    modals.close(MODAL_ID)
                },
                onError: notifyProblem,
            },
        )
    }

    return (
        <Stack>
            <TextInput
                label={<FieldLabel label="Name" help={TRACE_NAME_HELP} />}
                value={name}
                error={problem}
                data-autofocus
                onChange={(event) => setName(event.currentTarget.value)}
                onKeyDown={(event) => {
                    if (event.key === "Enter") save()
                }}
            />
            <TextInput
                label="Description"
                placeholder="Optional"
                value={description}
                onChange={(event) => setDescription(event.currentTarget.value)}
                onKeyDown={(event) => {
                    if (event.key === "Enter") save()
                }}
            />
            <Group justify="flex-end">
                <Button variant="default" onClick={() => modals.close(MODAL_ID)}>
                    Cancel
                </Button>
                <Button onClick={save} disabled={problem !== undefined} loading={edit.isPending}>
                    Save
                </Button>
            </Group>
        </Stack>
    )
}
