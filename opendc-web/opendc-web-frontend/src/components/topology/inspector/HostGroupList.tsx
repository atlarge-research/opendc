"use client"

import { formatMemory } from "@/components/format"
import { useHostTemplates } from "@/lib/api/catalogs"
import { hostCapacity } from "@/lib/topology/capacity"
import { type ClusterSpec, type HostSpec, cpuCount, gpuCount, hostCount, hostName } from "@/lib/topology/spec"
import { ActionIcon, Box, Button, Group, Menu, NumberInput, Paper, Stack, Text, Tooltip } from "@mantine/core"
import { IconCopy, IconCpu, IconPlus, IconTrash } from "@tabler/icons-react"

const ACCENTS = ["opendc", "grape", "teal", "orange", "indigo", "lime"]
const MAX_DRAWN_SLOTS = 40
const SLOT_GAP_PX = 2

export function HostGroupList({
    cluster,
    selectedHost,
    onSelectHost,
    onAddHost,
    onRemoveHost,
    onDuplicateHost,
    onCountChange,
}: {
    cluster: ClusterSpec
    selectedHost: number
    onSelectHost: (host: number) => void
    onAddHost: (host: HostSpec) => void
    onRemoveHost: (host: number) => void
    onDuplicateHost: (host: number) => void
    onCountChange: (host: number, count: number) => void
}) {
    const templates = useHostTemplates()
    const clusterHosts = cluster.hosts.reduce((total, entry) => total + hostCount(entry), 0)

    return (
        <Stack gap="xs">
            <Group justify="space-between">
                <Text fw={600} size="sm">
                    Host groups
                </Text>
                <Menu position="bottom-end" withinPortal>
                    <Menu.Target>
                        <Button size="compact-xs" variant="light" leftSection={<IconPlus size={14} />}>
                            Add
                        </Button>
                    </Menu.Target>
                    <Menu.Dropdown>
                        <Menu.Label>Start from a template</Menu.Label>
                        {(templates.data ?? []).map((template) => (
                            <Menu.Item
                                key={template.id}
                                leftSection={<IconCpu size={14} />}
                                onClick={() => onAddHost({ ...template.host, count: 1 })}
                            >
                                {template.label}
                            </Menu.Item>
                        ))}
                    </Menu.Dropdown>
                </Menu>
            </Group>

            {cluster.hosts.length === 0 && (
                <Text size="xs" c="dimmed">
                    This cluster has no hosts, which the simulator rejects. Add at least one group.
                </Text>
            )}

            {cluster.hosts.map((host, index) => (
                <HostGroupRow
                    key={`${hostName(host)}-${index}`}
                    host={host}
                    index={index}
                    selected={index === selectedHost}
                    onSelect={() => onSelectHost(index)}
                    onRemove={() => onRemoveHost(index)}
                    onDuplicate={() => onDuplicateHost(index)}
                    onCountChange={(count) => onCountChange(index, count)}
                    clusterHosts={clusterHosts}
                />
            ))}
        </Stack>
    )
}

function HostGroupRow({
    host,
    index,
    selected,
    onSelect,
    onRemove,
    onDuplicate,
    onCountChange,
    clusterHosts,
}: {
    host: HostSpec
    index: number
    selected: boolean
    onSelect: () => void
    onRemove: () => void
    onDuplicate: () => void
    onCountChange: (count: number) => void
    clusterHosts: number
}) {
    const count = hostCount(host)
    const capacity = hostCapacity(host)
    const accent = ACCENTS[index % ACCENTS.length] ?? "opendc"

    return (
        <Paper
            withBorder
            radius="sm"
            p="xs"
            onClick={onSelect}
            bg={selected ? "var(--mantine-color-default-hover)" : undefined}
            style={{ cursor: "pointer" }}
        >
            <Stack gap={6}>
                <Group justify="space-between" wrap="nowrap">
                    <Text size="sm" fw={500} lineClamp={1}>
                        {hostName(host)}
                    </Text>
                    <Group gap={4} wrap="nowrap">
                        <Tooltip label="Duplicate group">
                            <ActionIcon
                                size="sm"
                                variant="subtle"
                                color="gray"
                                aria-label={`Duplicate ${hostName(host)}`}
                                onClick={(event) => {
                                    event.stopPropagation()
                                    onDuplicate()
                                }}
                            >
                                <IconCopy size={14} />
                            </ActionIcon>
                        </Tooltip>
                        <ActionIcon
                            size="sm"
                            variant="subtle"
                            color="red"
                            aria-label={`Remove ${hostName(host)}`}
                            onClick={(event) => {
                                event.stopPropagation()
                                onRemove()
                            }}
                        >
                            <IconTrash size={14} />
                        </ActionIcon>
                    </Group>
                </Group>

                <Tooltip label={`${count} hosts, ${clusterHosts} in this cluster`} openDelay={300}>
                    <Group gap={4} wrap="nowrap" align="center">
                        <SlotStrip count={count} accent={accent} dim={!selected} />
                        {count > MAX_DRAWN_SLOTS && (
                            <Text size="xs" c="dimmed">
                                +{count - MAX_DRAWN_SLOTS}
                            </Text>
                        )}
                    </Group>
                </Tooltip>

                <Group justify="space-between" wrap="nowrap" align="flex-end">
                    <Stack gap={1}>
                        <Text size="xs" c="dimmed">
                            {capacity.cores / count} cores per host ({cpuCount(host.cpu)} x {host.cpu.coreCount})
                        </Text>
                        <Text size="xs" c="dimmed">
                            {formatMemory(capacity.memoryMiB / count)}
                            {host.gpu ? ` + ${gpuCount(host.gpu)} GPU` : ""}
                        </Text>
                    </Stack>
                    <NumberInput
                        size="xs"
                        w={84}
                        min={1}
                        max={9999}
                        label="Count"
                        value={count}
                        aria-label={`Host count for ${hostName(host)}`}
                        onClick={(event) => event.stopPropagation()}
                        onChange={(value) => onCountChange(typeof value === "number" ? value : Number(value) || 1)}
                    />
                </Group>
            </Stack>
        </Paper>
    )
}

// A repeating gradient rather than one node per host, so hundreds of hosts stay one element.
function SlotStrip({ count, accent, dim }: { count: number; accent: string; dim: boolean }) {
    const drawn = Math.min(Math.max(count, 1), MAX_DRAWN_SLOTS)
    const slot = 100 / drawn

    return (
        <Box
            h={14}
            flex={1}
            style={{
                borderRadius: 2,
                opacity: dim ? 0.65 : 1,
                backgroundImage: `repeating-linear-gradient(to right, var(--mantine-color-${accent}-6) 0 calc(${slot}% - ${SLOT_GAP_PX}px), transparent calc(${slot}% - ${SLOT_GAP_PX}px) ${slot}%)`,
            }}
        />
    )
}
