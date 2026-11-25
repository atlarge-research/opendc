"use client"

import { FieldLabel, UnitAdornment } from "@/components/util/FieldLabel"
import { DEFAULT_UNIT, type Quantity, type QuantityKind, type Unit, amountIn, parseQuantity } from "@/lib/units"
import { NumberInput } from "@mantine/core"

const KIND_NAMES: Record<QuantityKind, string> = {
    frequency: "frequency",
    dataSize: "size",
    dataRate: "data rate",
    power: "power",
    energy: "energy",
    time: "duration",
}

// Wide enough for the longest unit any field here prints, so the boxes line up in a column.
const UNIT_WIDTH = 48

// A number in one fixed unit; values stored in another unit are converted for display.
export function QuantityInput<K extends QuantityKind>({
    label,
    kind,
    unit = DEFAULT_UNIT[kind],
    value,
    onChange,
    allowUnset = false,
    help,
}: {
    label: string
    kind: K
    unit?: Unit<K>
    value: Quantity
    onChange: (next: string) => void
    allowUnset?: boolean
    help?: string
}) {
    const parsed = parseQuantity(kind, value)
    const invalid = parsed.status === "invalid" || (parsed.status === "unspecified" && !allowUnset)

    return (
        <NumberInput
            label={<FieldLabel label={label} help={help} />}
            size="xs"
            value={parsed.status === "ok" ? amountIn(kind, parsed.base, unit) : ""}
            placeholder={allowUnset ? "Unspecified" : undefined}
            min={0}
            allowNegative={false}
            hideControls
            error={invalid ? `Enter a ${KIND_NAMES[kind]}` : undefined}
            onChange={(next) => onChange(`${next === "" ? -1 : next} ${unit}`)}
            rightSection={<UnitAdornment unit={unit} />}
            rightSectionWidth={UNIT_WIDTH}
            rightSectionPointerEvents="none"
        />
    )
}
