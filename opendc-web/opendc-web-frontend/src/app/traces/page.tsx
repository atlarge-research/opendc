"use client"

import { AppShell } from "@/components/layout/AppShell"
import { openImportTrace } from "@/components/traces/ImportTraceModal"
import { TraceImports } from "@/components/traces/TraceImports"
import { TraceTable } from "@/components/traces/TraceTable"
import { openUploadTrace } from "@/components/traces/UploadTraceModal"
import { TableGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { useTraces } from "@/lib/api/traces"
import type { TraceKind } from "@/lib/api/types"
import { Button, Container, Group, SegmentedControl, Stack, Text, Title } from "@mantine/core"
import { IconLink, IconUpload } from "@tabler/icons-react"
import { useState } from "react"

const ALL = "all"

export default function TracesPage() {
    const [kind, setKind] = useState<TraceKind | typeof ALL>(ALL)
    const traces = useTraces(kind === ALL ? undefined : kind)

    return (
        <AppShell>
            <Container size="lg" py="lg">
                <Stack gap="lg">
                    <Stack gap={4} py={25}>
                        <Title order={1}>Traces</Title>
                        <Text c="dimmed">
                            The workloads, carbon intensities and failure records your experiments can run against.
                        </Text>
                    </Stack>
                    <Group justify="space-between">
                        <SegmentedControl
                            value={kind}
                            onChange={(value) => setKind(value as TraceKind | typeof ALL)}
                            data={[
                                { value: ALL, label: "All" },
                                { value: "workload", label: "Workload" },
                                { value: "carbon", label: "Carbon" },
                                { value: "failure", label: "Failure" },
                            ]}
                        />
                        <Group gap="xs">
                            <Button variant="default" leftSection={<IconLink size={16} />} onClick={openImportTrace}>
                                Import from URL
                            </Button>
                            <Button leftSection={<IconUpload size={16} />} onClick={openUploadTrace}>
                                Upload trace
                            </Button>
                        </Group>
                    </Group>
                    <TraceImports />
                    <QueryState query={traces} ghost={<TableGhost columns={5} rows={4} />}>
                        {(loaded) => <TraceTable traces={loaded} />}
                    </QueryState>
                </Stack>
            </Container>
        </AppShell>
    )
}
