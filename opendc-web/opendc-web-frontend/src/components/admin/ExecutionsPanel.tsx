"use client"

import { ExecutionDrawer } from "@/components/admin/ExecutionDrawer"
import { ExecutionTable } from "@/components/admin/ExecutionTable"
import { TableGhost } from "@/components/util/Ghost"
import { QueryState } from "@/components/util/QueryState"
import { STATE_BADGES } from "@/lib/admin/phase"
import { ADMIN_PAGE_SIZE, EXECUTION_STATES, type StateFilter, useAdminExecutions } from "@/lib/api/admin"
import { Group, Pagination, Select, Stack } from "@mantine/core"
import { useState } from "react"

const FILTERS: { value: StateFilter; label: string }[] = [
    { value: "live", label: "Live" },
    { value: "all", label: "All" },
    ...EXECUTION_STATES.map((state) => ({ value: state, label: STATE_BADGES[state].label })),
]

export function ExecutionsPanel() {
    const [filter, setFilter] = useState<StateFilter>("live")
    const [page, setPage] = useState(1)
    const [opened, setOpened] = useState<string | undefined>(undefined)
    const executions = useAdminExecutions(filter, page)
    const pages = Math.max(1, Math.ceil((executions.data?.total ?? 0) / ADMIN_PAGE_SIZE))

    return (
        <Stack gap="md">
            <Group justify="space-between">
                <Select
                    aria-label="Execution state"
                    data={FILTERS}
                    value={filter}
                    allowDeselect={false}
                    w={180}
                    onChange={(value) => {
                        setFilter(FILTERS.find((option) => option.value === value)?.value ?? "live")
                        setPage(1)
                    }}
                />
                {pages > 1 && <Pagination total={pages} value={page} onChange={setPage} size="sm" />}
            </Group>
            <QueryState query={executions} ghost={<TableGhost columns={5} rows={4} />}>
                {(loaded) => <ExecutionTable executions={loaded.items} onOpen={setOpened} />}
            </QueryState>
            <ExecutionDrawer id={opened} onClose={() => setOpened(undefined)} />
        </Stack>
    )
}
