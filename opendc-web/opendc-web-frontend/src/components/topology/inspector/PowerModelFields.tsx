"use client"

import { starterPowerModel } from "@/components/topology/defaults"
import { CatalogSelect } from "@/components/topology/inspector/CatalogSelect"
import { QuantityInput } from "@/components/topology/inspector/QuantityInput"
import { FieldLabel } from "@/components/util/FieldLabel"
import { useCatalog } from "@/lib/api/catalogs"
import { type PowerSpec, isPowerModelType } from "@/lib/topology/spec"
import { Group, NumberInput, Stack } from "@mantine/core"

export const DEFAULT_POWER_MODEL: PowerSpec = { type: "linear", maxPower: "400 Watts", idlePower: "120 Watts" }

export function PowerModelFields({
    model,
    onChange,
}: {
    model: PowerSpec
    onChange: (next: PowerSpec) => void
}) {
    const powerModels = useCatalog("power-models")

    return (
        <Stack gap="xs">
            <CatalogSelect
                label="Power model"
                entries={powerModels}
                value={model.type}
                onChange={(value) => value && isPowerModelType(value) && onChange(starterPowerModel(value, model))}
            />
            <DrawFields model={model} onChange={onChange} />
            <ShapeField model={model} onChange={onChange} />
        </Stack>
    )
}

function DrawFields({ model, onChange }: { model: PowerSpec; onChange: (next: PowerSpec) => void }) {
    if (model.type === "constant") {
        return (
            <QuantityInput
                label="Draw"
                kind="power"
                value={model.power}
                onChange={(power) => onChange({ ...model, power })}
            />
        )
    }
    return (
        <Group grow gap="xs" align="flex-start">
            <QuantityInput
                label="Peak"
                kind="power"
                value={model.maxPower}
                onChange={(maxPower) => onChange({ ...model, maxPower })}
            />
            <QuantityInput
                label="Idle"
                kind="power"
                value={model.idlePower}
                onChange={(idlePower) => onChange({ ...model, idlePower })}
            />
        </Group>
    )
}

/** The one parameter a fitted curve has beyond its peak and idle draw. */
function ShapeField({ model, onChange }: { model: PowerSpec; onChange: (next: PowerSpec) => void }) {
    switch (model.type) {
        case "mse":
            return (
                <NumberInput
                    label={<FieldLabel label="Calibration factor" help="Exponent fitted to measured power draw." />}
                    size="xs"
                    min={0}
                    decimalScale={3}
                    value={model.calibrationFactor}
                    onChange={(value) => onChange({ ...model, calibrationFactor: Number(value) || 0 })}
                />
            )
        case "asymptotic":
            return (
                <NumberInput
                    label={
                        <FieldLabel
                            label="Asymptotic utilization"
                            help="Utilization beyond which draw grows close to linearly, typically between 0.2 and 0.5."
                        />
                    }
                    size="xs"
                    min={0}
                    max={1}
                    decimalScale={3}
                    value={model.asymUtil}
                    onChange={(value) => onChange({ ...model, asymUtil: Number(value) || 0 })}
                />
            )
        case "constant":
        case "linear":
        case "square":
        case "cubic":
        case "sqrt":
            return null
    }
}
