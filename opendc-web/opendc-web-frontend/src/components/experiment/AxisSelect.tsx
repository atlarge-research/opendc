"use client"

import type { AxisChoice } from "@/components/experiment/draftAxes"
import { FieldLabel } from "@/components/util/FieldLabel"
import { MultiSelect, Text, Tooltip } from "@mantine/core"

export function AxisSelect({
    label,
    help,
    choices,
    value,
    onChange,
}: {
    label: string
    help: string
    choices: AxisChoice[]
    value: string[]
    onChange: (value: string[]) => void
}) {
    const descriptions = new Map(choices.map((choice) => [choice.value, choice.description]))

    return (
        <MultiSelect
            label={<FieldLabel label={label} help={help} />}
            size="sm"
            data={choices.map((choice) => ({ value: choice.value, label: choice.label }))}
            value={value}
            onChange={onChange}
            placeholder={value.length === 0 ? "Pick at least one" : undefined}
            searchable
            hidePickedOptions
            nothingFoundMessage="Nothing left to add"
            renderOption={({ option }) => {
                const description = descriptions.get(option.value)
                if (!description) return <Text size="sm">{option.label}</Text>
                return (
                    <Tooltip label={description} position="right" withArrow multiline w={280} openDelay={200}>
                        <Text size="sm" w="100%">
                            {option.label}
                        </Text>
                    </Tooltip>
                )
            }}
        />
    )
}
