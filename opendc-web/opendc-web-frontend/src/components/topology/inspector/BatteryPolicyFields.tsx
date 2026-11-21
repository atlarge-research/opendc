"use client"

import { FieldLabel, UnitAdornment } from "@/components/util/FieldLabel"
import type { BatteryPolicy } from "@/lib/topology/spec"
import { SCALAR_UNIT } from "@/lib/units"
import { Group, NumberInput } from "@mantine/core"
import type { ReactNode } from "react"

export function BatteryPolicyFields({
    policy,
    onChange,
}: {
    policy: BatteryPolicy
    onChange: (next: BatteryPolicy) => void
}) {
    switch (policy.type) {
        case "single":
            return thresholdInput(
                "Carbon threshold",
                "Carbon intensity at or above which the battery discharges instead of drawing from the source. Below it, the battery charges.",
                policy.carbonThreshold,
                (carbonThreshold) => onChange({ ...policy, carbonThreshold }),
            )
        case "double":
            return (
                <Group grow gap="xs">
                    {thresholdInput(
                        "Lower",
                        "Below this carbon intensity the battery charges.",
                        policy.lowerThreshold,
                        (lowerThreshold) => onChange({ ...policy, lowerThreshold }),
                    )}
                    {thresholdInput(
                        "Upper",
                        "Above this carbon intensity the battery discharges.",
                        policy.upperThreshold,
                        (upperThreshold) => onChange({ ...policy, upperThreshold }),
                    )}
                </Group>
            )
        case "runningMean":
        case "runningMeanPlus":
            return (
                <Group grow gap="xs">
                    {thresholdInput(
                        "Starting threshold",
                        "Carbon intensity used before enough samples have been seen to compute the running statistic.",
                        policy.startingThreshold,
                        (startingThreshold) => onChange({ ...policy, startingThreshold }),
                    )}
                    <NumberInput
                        label={
                            <FieldLabel
                                label="Window size"
                                help="How many recent carbon intensity samples the running statistic is computed over."
                            />
                        }
                        size="xs"
                        min={1}
                        value={policy.windowSize}
                        onChange={(value) => onChange({ ...policy, windowSize: Number(value) || 1 })}
                    />
                </Group>
            )
    }
}

/** Every battery policy is steered by carbon intensities, which all read in the same unit. */
function thresholdInput(label: string, help: string, value: number, onChange: (next: number) => void): ReactNode {
    return (
        <NumberInput
            label={<FieldLabel label={label} help={help} />}
            size="xs"
            value={value}
            onChange={(next) => onChange(Number(next) || 0)}
            rightSection={<UnitAdornment unit={SCALAR_UNIT.carbonIntensity} />}
            rightSectionWidth={70}
            rightSectionPointerEvents="none"
        />
    )
}
