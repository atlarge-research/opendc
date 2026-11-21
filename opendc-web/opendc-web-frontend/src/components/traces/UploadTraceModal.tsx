"use client"

import { formatBytes } from "@/components/format"
import { TRACE_NAME_HELP, traceNameProblem } from "@/components/traces/traceName"
import { FieldLabel } from "@/components/util/FieldLabel"
import { notifyProblem } from "@/components/util/feedback"
import { useTraceKinds } from "@/lib/api/traces"
import type { TraceKind } from "@/lib/api/types"
import { type UploadProgress, useUploadTrace } from "@/lib/api/uploads"
import { Button, FileInput, Group, Progress, Select, Stack, Text, TextInput } from "@mantine/core"
import { modals } from "@mantine/modals"
import { IconFileCheck, IconUpload } from "@tabler/icons-react"
import { useRef, useState } from "react"

const MODAL_ID = "upload-trace"

// Keyed by table name, so a table the server adds later falls through to the general note.
const TABLE_HELP: Record<string, string> = {
    tasks: "Upload a parquet file with one row per task: its id, when it is submitted, how long it runs and what it asks for in cores and memory.",
    fragments:
        "Upload a parquet file with one row per slice of a task's run, giving the cores and the CPU usage over that stretch. A task is usually many fragments.",
    carbon: "Upload a parquet file with one row per reading: a timestamp and the carbon intensity of the grid at that moment.",
    failures:
        "Upload a parquet file with one row per outage: when it begins, how long it lasts and how much of the datacentre it takes down.",
}

const UNKNOWN_TABLE_HELP = "Upload a parquet file holding this table, with the columns the simulator reads it by."

export function openUploadTrace(): void {
    modals.open({ modalId: MODAL_ID, title: "Upload a trace", children: <UploadTraceForm /> })
}

function UploadProgressBar({ progress }: { progress: UploadProgress }) {
    const sending = progress.sent < progress.total
    const percent = Math.round((progress.sent / progress.total) * 100)

    return (
        <Stack gap={6}>
            <Group justify="space-between" wrap="nowrap">
                <Text size="xs" c="dimmed">
                    {sending ? `Uploading ${formatBytes(progress.total)}` : "Checking the tables"}
                </Text>
                <Text size="xs" c="dimmed">
                    {sending ? `${percent}%` : ""}
                </Text>
            </Group>
            <Progress value={sending ? percent : 100} size="sm" radius="xl" animated transitionDuration={200} />
        </Stack>
    )
}

function UploadTraceForm() {
    const kinds = useTraceKinds()
    const upload = useUploadTrace()
    const [kind, setKind] = useState<TraceKind>("workload")
    const [name, setName] = useState("")
    const [description, setDescription] = useState("")
    const [files, setFiles] = useState<Record<string, File>>({})
    const [progress, setProgress] = useState<UploadProgress>({ sent: 0, total: 0 })
    const abort = useRef<AbortController | null>(null)

    const tables = kinds.data?.find((entry) => entry.kind === kind)?.tables ?? []
    const nameProblem = traceNameProblem(name)
    const complete =
        nameProblem === undefined && tables.length > 0 && tables.every((table) => files[table] !== undefined)

    const submit = () => {
        const controller = new AbortController()
        abort.current = controller
        upload.mutate(
            { kind, name: name.trim(), description, files, onProgress: setProgress, signal: controller.signal },
            {
                onSuccess: () => modals.close(MODAL_ID),
                // A cancel is not a failure to report: the reader asked for it.
                onError: (error) => {
                    if (!controller.signal.aborted) notifyProblem(error)
                },
            },
        )
    }

    // Aborting fails the upload, which discards the half-made trace; closing alone would leave it running.
    const cancel = () => {
        abort.current?.abort()
        modals.close(MODAL_ID)
    }

    return (
        <Stack>
            <Select
                label="Kind"
                data={(kinds.data ?? []).map((entry) => entry.kind)}
                value={kind}
                allowDeselect={false}
                onChange={(value) => {
                    if (value === null) return
                    setKind(value as TraceKind)
                    setFiles({})
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
            {tables.map((table) => (
                <FileInput
                    key={table}
                    label={<FieldLabel label={`${table}.parquet`} help={TABLE_HELP[table] ?? UNKNOWN_TABLE_HELP} />}
                    placeholder="Choose a file"
                    accept=".parquet"
                    clearable
                    leftSection={files[table] ? <IconFileCheck size={16} /> : <IconUpload size={16} />}
                    description={files[table] ? formatBytes(files[table].size) : undefined}
                    value={files[table] ?? null}
                    onChange={(file) =>
                        setFiles((current) => {
                            if (file === null) {
                                const { [table]: _removed, ...rest } = current
                                return rest
                            }
                            return { ...current, [table]: file }
                        })
                    }
                />
            ))}
            {upload.isPending && progress.total > 0 && <UploadProgressBar progress={progress} />}
            <Group justify="flex-end">
                <Button variant="default" onClick={cancel}>
                    Cancel
                </Button>
                <Button onClick={submit} disabled={!complete} loading={upload.isPending}>
                    Upload
                </Button>
            </Group>
        </Stack>
    )
}
