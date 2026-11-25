"use client"

import { AXIS_HELP, AXIS_LABELS } from "@/components/experiment/axisLabels"
import { FieldLabel } from "@/components/util/FieldLabel"
import { TagsInput } from "@mantine/core"

export function FailureBudgetAxis({
    entries,
    onChange,
}: Readonly<{ entries: number[]; onChange: (next: number[]) => void }>) {
    return (
        <TagsInput
            label={<FieldLabel label={AXIS_LABELS.maxNumFailures} help={AXIS_HELP.maxNumFailures} />}
            size="sm"
            value={entries.map(String)}
            onChange={(values) => onChange(budgets(values))}
            splitChars={[",", " "]}
            placeholder={entries.length === 0 ? "Add a limit" : undefined}
            clearable
        />
    )
}

function budgets(values: string[]): number[] {
    return values.flatMap((value) => {
        const count = Number(value)
        return Number.isInteger(count) && count >= 1 ? [count] : []
    })
}
