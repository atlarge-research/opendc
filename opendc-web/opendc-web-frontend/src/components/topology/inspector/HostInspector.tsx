"use client"

import { DEFAULT_POWER_MODEL, PowerModelFields } from "@/components/topology/inspector/PowerModelFields"
import { QuantityInput } from "@/components/topology/inspector/QuantityInput"
import type { GpuSpec, HostSpec } from "@/lib/topology/spec"
import { hostName } from "@/lib/topology/spec"
import { Accordion, Group, NumberInput, Stack, Switch, TextInput } from "@mantine/core"

const STARTER_GPU: GpuSpec = {
    coreCount: 6912,
    coreSpeed: "1.4 GHz",
    count: 1,
    memory: "40 GiB",
    memoryBandwidth: "1600 GBps",
    vendor: "NVIDIA",
    modelName: "A100",
}

export function HostInspector({
    host,
    onChange,
}: {
    host: HostSpec
    onChange: (next: HostSpec) => void
}) {
    return (
        <Stack gap="sm">
            <TextInput
                label="Group name"
                size="xs"
                value={hostName(host)}
                onChange={(event) => onChange({ ...host, name: event.currentTarget.value })}
            />

            <Accordion defaultValue="cpu" variant="separated" radius="sm">
                <Accordion.Item value="cpu">
                    <Accordion.Control>CPU</Accordion.Control>
                    <Accordion.Panel>
                        <Stack gap="xs">
                            <Group grow gap="xs">
                                <NumberInput
                                    label="Cores per CPU"
                                    size="xs"
                                    min={1}
                                    value={host.cpu.coreCount}
                                    error={host.cpu.coreCount <= 0 ? "Must be > 0" : undefined}
                                    onChange={(value) =>
                                        onChange({ ...host, cpu: { ...host.cpu, coreCount: numberOf(value) } })
                                    }
                                />
                                <NumberInput
                                    label="CPUs per host"
                                    size="xs"
                                    min={1}
                                    value={host.cpu.count ?? 1}
                                    onChange={(value) =>
                                        onChange({ ...host, cpu: { ...host.cpu, count: numberOf(value) } })
                                    }
                                />
                            </Group>
                            <QuantityInput
                                label="Core speed"
                                kind="frequency"
                                value={host.cpu.coreSpeed}
                                onChange={(coreSpeed) => onChange({ ...host, cpu: { ...host.cpu, coreSpeed } })}
                            />
                            <Group grow gap="xs">
                                <TextInput
                                    label="Vendor"
                                    size="xs"
                                    value={host.cpu.vendor ?? ""}
                                    onChange={(event) =>
                                        onChange({ ...host, cpu: { ...host.cpu, vendor: event.currentTarget.value } })
                                    }
                                />
                                <TextInput
                                    label="Model"
                                    size="xs"
                                    value={host.cpu.modelName ?? ""}
                                    onChange={(event) =>
                                        onChange({
                                            ...host,
                                            cpu: { ...host.cpu, modelName: event.currentTarget.value },
                                        })
                                    }
                                />
                            </Group>
                            <PowerModelFields
                                model={host.cpuPowerModel ?? DEFAULT_POWER_MODEL}
                                onChange={(cpuPowerModel) => onChange({ ...host, cpuPowerModel })}
                            />
                        </Stack>
                    </Accordion.Panel>
                </Accordion.Item>

                <Accordion.Item value="memory">
                    <Accordion.Control>Memory</Accordion.Control>
                    <Accordion.Panel>
                        <Stack gap="xs">
                            <QuantityInput
                                label="Size"
                                kind="dataSize"
                                value={host.memory.size}
                                onChange={(size) => onChange({ ...host, memory: { ...host.memory, size } })}
                            />
                            <QuantityInput
                                label="Speed"
                                kind="frequency"
                                allowUnset
                                value={host.memory.speed ?? -1}
                                onChange={(speed) => onChange({ ...host, memory: { ...host.memory, speed } })}
                            />
                        </Stack>
                    </Accordion.Panel>
                </Accordion.Item>

                <Accordion.Item value="gpu">
                    <Accordion.Control>GPU</Accordion.Control>
                    <Accordion.Panel>
                        <Stack gap="xs">
                            <Switch
                                size="xs"
                                label="This host has GPUs"
                                checked={host.gpu !== undefined && host.gpu !== null}
                                onChange={(event) =>
                                    onChange({
                                        ...host,
                                        gpu: event.currentTarget.checked ? STARTER_GPU : undefined,
                                        gpuPowerModel: event.currentTarget.checked
                                            ? (host.gpuPowerModel ?? DEFAULT_POWER_MODEL)
                                            : host.gpuPowerModel,
                                    })
                                }
                            />
                            {host.gpu && (
                                <GpuFields
                                    gpu={host.gpu}
                                    onChange={(gpu) => onChange({ ...host, gpu })}
                                    powerModel={host.gpuPowerModel ?? DEFAULT_POWER_MODEL}
                                    onPowerChange={(gpuPowerModel) => onChange({ ...host, gpuPowerModel })}
                                />
                            )}
                        </Stack>
                    </Accordion.Panel>
                </Accordion.Item>
            </Accordion>
        </Stack>
    )
}

function GpuFields({
    gpu,
    onChange,
    powerModel,
    onPowerChange,
}: {
    gpu: GpuSpec
    onChange: (next: GpuSpec) => void
    powerModel: Parameters<typeof PowerModelFields>[0]["model"]
    onPowerChange: (next: Parameters<typeof PowerModelFields>[0]["model"]) => void
}) {
    return (
        <Stack gap="xs">
            <Group grow gap="xs">
                <NumberInput
                    label="Cores"
                    size="xs"
                    min={1}
                    value={gpu.coreCount}
                    onChange={(value) => onChange({ ...gpu, coreCount: numberOf(value) })}
                />
                <NumberInput
                    label="GPUs per host"
                    size="xs"
                    min={1}
                    value={gpu.count ?? 1}
                    onChange={(value) => onChange({ ...gpu, count: numberOf(value) })}
                />
            </Group>
            <QuantityInput
                label="Core speed"
                kind="frequency"
                value={gpu.coreSpeed}
                onChange={(coreSpeed) => onChange({ ...gpu, coreSpeed })}
            />
            <QuantityInput
                label="Memory"
                kind="dataSize"
                allowUnset
                value={gpu.memory ?? -1}
                onChange={(memory) => onChange({ ...gpu, memory })}
            />
            <QuantityInput
                label="Memory bandwidth"
                kind="dataRate"
                allowUnset
                value={gpu.memoryBandwidth ?? -1}
                onChange={(memoryBandwidth) => onChange({ ...gpu, memoryBandwidth })}
            />
            <PowerModelFields model={powerModel} onChange={onPowerChange} />
        </Stack>
    )
}

function numberOf(value: string | number): number {
    return typeof value === "number" ? value : (Number(value) ?? 0)
}
