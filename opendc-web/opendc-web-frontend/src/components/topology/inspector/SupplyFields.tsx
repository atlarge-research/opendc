"use client"

import { formatPower } from "@/components/format"
import { CatalogSelect } from "@/components/topology/inspector/CatalogSelect"
import { QuantityInput } from "@/components/topology/inspector/QuantityInput"
import { useTraceOptions } from "@/lib/api/traces"
import { WATTS_PER_KW, dataCenterHeadroom, isOverBudget, suggestedSupplyW } from "@/lib/topology/capacity"
import { type DataCenterSpec, type PowerSourceSpec, carbonTraceName } from "@/lib/topology/spec"
import { Progress, Stack, Switch, Text } from "@mantine/core"

export function SupplyFields({
    dataCenter,
    onChange,
}: {
    dataCenter: DataCenterSpec
    onChange: (powerSource: PowerSourceSpec) => void
}) {
    const source = dataCenter.powerSource ?? {}
    const headroom = dataCenterHeadroom(dataCenter)
    const budget = headroom.budget
    const carbon = carbonTraceName(source.carbon)
    const carbonTraces = useTraceOptions("carbon")

    return (
        <Stack gap="xs">
            <Switch
                size="xs"
                label="Limit the supply"
                checked={budget.status === "limited"}
                onChange={(event) =>
                    onChange({
                        ...source,
                        maxPower: event.currentTarget.checked
                            ? `${suggestedSupplyW(dataCenter) / WATTS_PER_KW} kWatts`
                            : undefined,
                    })
                }
            />

            {budget.status === "limited" && (
                <>
                    <QuantityInput
                        label="Supply limit"
                        kind="power"
                        unit="kW"
                        help="Most power this data center can draw. Every cluster on its floor shares it."
                        value={source.maxPower ?? budget.watts}
                        onChange={(maxPower) => onChange({ ...source, maxPower })}
                    />
                    <Progress
                        size="sm"
                        value={budget.watts > 0 ? Math.min(100, (headroom.usedW / budget.watts) * 100) : 100}
                        color={isOverBudget(headroom) ? "red" : "opendc"}
                        aria-label="Peak draw against the supply"
                    />
                    <Text size="xs" c={isOverBudget(headroom) ? "red" : "dimmed"}>
                        Peak draw {formatPower(headroom.usedW)} of {formatPower(budget.watts)}
                    </Text>
                </>
            )}

            <CatalogSelect
                label="Carbon intensity trace"
                entries={carbonTraces}
                clearable
                placeholder="None"
                value={carbon}
                onChange={(value) =>
                    onChange({
                        ...source,
                        carbon: value ? { type: "trace", source: { type: "named", name: value } } : undefined,
                    })
                }
            />
        </Stack>
    )
}
