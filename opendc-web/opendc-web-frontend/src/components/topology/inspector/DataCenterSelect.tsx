"use client"

import { FieldLabel } from "@/components/util/FieldLabel"
import { Select } from "@mantine/core"

/** Picks a data center by its position, since names may repeat. */
export function DataCenterSelect({
    label,
    names,
    value,
    onChange,
}: {
    label: string
    names: string[]
    value: number
    onChange: (dataCenter: number) => void
}) {
    return (
        <Select
            label={<FieldLabel label={label} help="Moves the cluster onto that data center's floor and supply." />}
            size="xs"
            data={names.map((name, index) => ({ value: String(index), label: `${index + 1}. ${name}` }))}
            value={String(value)}
            allowDeselect={false}
            onChange={(chosen) => chosen !== null && onChange(Number(chosen))}
        />
    )
}
