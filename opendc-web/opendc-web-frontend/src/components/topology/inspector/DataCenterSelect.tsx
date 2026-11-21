"use client"

import { FieldLabel } from "@/components/util/FieldLabel"
import { Select } from "@mantine/core"

/** Picks a data center by its place, since names may repeat. */
export function DataCenterSelect({
    label,
    names,
    value,
    onChange,
}: {
    label: string
    names: string[]
    value: number | null
    onChange: (dataCenter: number) => void
}) {
    return (
        <Select
            label={<FieldLabel label={label} help="Moves the cluster onto that data center's floor and supply." />}
            size="xs"
            data={names.map((name, index) => ({ value: String(index), label: `${index + 1}. ${name}` }))}
            value={value === null ? null : String(value)}
            placeholder="Choose a data center"
            allowDeselect={false}
            onChange={(chosen) => chosen !== null && onChange(Number(chosen))}
        />
    )
}
