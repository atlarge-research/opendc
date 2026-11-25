"use client"

import { isBatteryPolicyType, starterBattery, starterBatteryPolicy } from "@/components/topology/defaults"
import { BatteryPolicyFields } from "@/components/topology/inspector/BatteryPolicyFields"
import { CatalogSelect } from "@/components/topology/inspector/CatalogSelect"
import { QuantityInput } from "@/components/topology/inspector/QuantityInput"
import { useCatalog } from "@/lib/api/catalogs"
import type { BatterySpec } from "@/lib/topology/spec"
import { Group, Stack, Switch } from "@mantine/core"

/** A data center's battery, which charges from and discharges into the supply its clusters share. */
export function BatteryFields({
    battery,
    onChange,
}: {
    battery: BatterySpec | null | undefined
    onChange: (battery: BatterySpec | undefined) => void
}) {
    const policies = useCatalog("battery-policies")

    return (
        <Stack gap="xs">
            <Switch
                size="xs"
                label="Battery attached"
                checked={battery !== undefined && battery !== null}
                onChange={(event) => onChange(event.currentTarget.checked ? starterBattery() : undefined)}
            />

            {battery && (
                <>
                    <Group grow gap="xs" align="flex-start">
                        <QuantityInput
                            label="Capacity"
                            kind="energy"
                            help="Energy the battery holds when full."
                            value={battery.capacity}
                            onChange={(capacity) => onChange({ ...battery, capacity })}
                        />
                        <QuantityInput
                            label="Charge rate"
                            kind="power"
                            help="How fast the battery draws power while charging."
                            value={battery.chargingSpeed}
                            onChange={(chargingSpeed) => onChange({ ...battery, chargingSpeed })}
                        />
                    </Group>
                    <CatalogSelect
                        label="Policy"
                        entries={policies}
                        value={battery.policy.type}
                        onChange={(value) =>
                            value &&
                            isBatteryPolicyType(value) &&
                            onChange({ ...battery, policy: starterBatteryPolicy(value) })
                        }
                    />
                    <BatteryPolicyFields
                        policy={battery.policy}
                        onChange={(policy) => onChange({ ...battery, policy })}
                    />
                </>
            )}
        </Stack>
    )
}
