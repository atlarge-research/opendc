"use client"

import { TRACE_NAME_HELP, traceNameProblem } from "@/components/traces/traceName"
import { FieldLabel } from "@/components/util/FieldLabel"
import { notifyProblem } from "@/components/util/feedback"
import { useStartImport, useTraceKinds } from "@/lib/api/traces"
import type { TraceKind } from "@/lib/api/types"
import { Button, Group, Select, Stack, Text, TextInput } from "@mantine/core"
import { modals } from "@mantine/modals"
import { IconLink } from "@tabler/icons-react"
import { useState } from "react"

const MODAL_ID = "import-trace"

export function openImportTrace(): void {
    modals.open({ modalId: MODAL_ID, title: "Import a trace from URLs", children: <ImportTraceForm /> })
}

/** Whether [value] reads as a web address, the one thing the server will fetch. */
function isWebUrl(value: string): boolean {
    try {
        const protocol = new URL(value.trim()).protocol
        return protocol === "http:" || protocol === "https:"
    } catch {
        return false
    }
}

function ImportTraceForm() {
    const kinds = useTraceKinds()
    const start = useStartImport()
    const [kind, setKind] = useState<TraceKind>("workload")
    const [name, setName] = useState("")
    const [description, setDescription] = useState("")
    const [sources, setSources] = useState<Record<string, string>>({})

    const tables = kinds.data?.find((entry) => entry.kind === kind)?.tables ?? []
    const nameProblem = traceNameProblem(name)
    const complete =
        nameProblem === undefined && tables.length > 0 && tables.every((table) => isWebUrl(sources[table] ?? ""))

    const submit = () =>
        start.mutate(
            { kind, name: name.trim(), description, sources },
            { onSuccess: () => modals.close(MODAL_ID), onError: notifyProblem },
        )

    return (
        <Stack>
            <Text size="sm" c="dimmed">
                The server fetches each file itself and adds the trace to your library once every table has arrived and
                checks out. You can close this and keep working meanwhile.
            </Text>
            <Select
                label="Kind"
                data={(kinds.data ?? []).map((entry) => entry.kind)}
                value={kind}
                allowDeselect={false}
                onChange={(value) => {
                    if (value === null) return
                    setKind(value as TraceKind)
                    setSources({})
                }}
            />
            <TextInput
                label={<FieldLabel label="Name" help={TRACE_NAME_HELP} />}
                value={name}
                error={name === "" ? undefined : nameProblem}
                onChange={(event) => setName(event.currentTarget.value)}
            />
            <TextInput
                label="Description"
                placeholder="Optional"
                value={description}
                onChange={(event) => setDescription(event.currentTarget.value)}
            />
            {tables.map((table) => {
                const url = sources[table] ?? ""
                return (
                    <TextInput
                        key={table}
                        label={`${table}.parquet`}
                        placeholder="https://"
                        leftSection={<IconLink size={16} />}
                        value={url}
                        error={url === "" || isWebUrl(url) ? undefined : "Enter an http or https URL"}
                        onChange={(event) => {
                            const value = event.currentTarget.value
                            setSources((current) => ({ ...current, [table]: value }))
                        }}
                    />
                )
            })}
            <Group justify="flex-end">
                <Button variant="default" onClick={() => modals.close(MODAL_ID)}>
                    Cancel
                </Button>
                <Button onClick={submit} disabled={!complete} loading={start.isPending}>
                    Import
                </Button>
            </Group>
        </Stack>
    )
}
