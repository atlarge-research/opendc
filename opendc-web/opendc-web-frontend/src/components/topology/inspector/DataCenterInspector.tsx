"use client"

import { BatteryFields } from "@/components/topology/inspector/BatteryFields"
import { IssueList } from "@/components/topology/inspector/IssueList"
import { SupplyFields } from "@/components/topology/inspector/SupplyFields"
import type { DocumentIssue } from "@/lib/api/types"
import { type DataCenterSpec, dataCenterName } from "@/lib/topology/spec"
import { dataCenterPath, issuesUnder } from "@/lib/topology/validation"
import { Badge, Box, Button, Group, ScrollArea, Stack, Tabs, Text, TextInput } from "@mantine/core"
import { modals } from "@mantine/modals"
import { IconCopy, IconTrash } from "@tabler/icons-react"

export function DataCenterInspector({
    dataCenter,
    index,
    removable,
    issues,
    onChange,
    onDuplicate,
    onRemove,
}: {
    dataCenter: DataCenterSpec
    index: number
    removable: boolean
    issues: DocumentIssue[]
    onChange: (patch: Partial<DataCenterSpec>) => void
    onDuplicate: () => void
    onRemove: () => void
}) {
    const path = dataCenterPath(index)
    const name = dataCenterName(dataCenter)

    const confirmRemove = () => {
        if (dataCenter.clusters.length === 0) return onRemove()
        modals.openConfirmModal({
            title: `Delete ${name}?`,
            children: <Text size="sm">Its {dataCenter.clusters.length} clusters are deleted with it.</Text>,
            labels: { confirm: "Delete data center", cancel: "Keep it" },
            confirmProps: { color: "red" },
            onConfirm: onRemove,
        })
    }

    return (
        <Stack h="100%" gap={0}>
            <Box px="md" pt="md" pb="xs">
                <Group justify="space-between" wrap="nowrap">
                    <Text fw={600} lineClamp={1}>
                        {name}
                    </Text>
                    <Badge variant="light" color="gray" size="sm">
                        {dataCenter.clusters.length} clusters
                    </Badge>
                </Group>
            </Box>

            <ScrollArea style={{ flex: 1, minHeight: 0 }}>
                <Stack gap="sm" p="md" pt={0}>
                    <IssueList issues={issuesUnder(issues, path)} prefix={`${path}.`} />
                    <TextInput
                        label="Name"
                        size="xs"
                        value={name}
                        onChange={(event) => onChange({ name: event.currentTarget.value })}
                    />
                    <Tabs defaultValue="power">
                        <Tabs.List>
                            <Tabs.Tab value="power">Power</Tabs.Tab>
                            <Tabs.Tab value="battery">Battery</Tabs.Tab>
                        </Tabs.List>
                        <Tabs.Panel value="power" pt="sm">
                            <SupplyFields
                                dataCenter={dataCenter}
                                onChange={(powerSource) => onChange({ powerSource })}
                            />
                        </Tabs.Panel>
                        <Tabs.Panel value="battery" pt="sm">
                            <BatteryFields battery={dataCenter.battery} onChange={(battery) => onChange({ battery })} />
                        </Tabs.Panel>
                    </Tabs>
                    <Group gap="xs">
                        <Button size="xs" variant="default" leftSection={<IconCopy size={14} />} onClick={onDuplicate}>
                            Duplicate
                        </Button>
                        <Button
                            size="xs"
                            variant="default"
                            color="red"
                            leftSection={<IconTrash size={14} />}
                            disabled={!removable}
                            onClick={confirmRemove}
                        >
                            Delete
                        </Button>
                    </Group>
                </Stack>
            </ScrollArea>
        </Stack>
    )
}
