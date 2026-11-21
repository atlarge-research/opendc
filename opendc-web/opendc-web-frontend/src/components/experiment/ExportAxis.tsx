"use client"

import { AXIS_HELP } from "@/components/experiment/axisLabels"
import { FieldLabel, UnitAdornment } from "@/components/util/FieldLabel"
import { DEFAULT_EXPORT_INTERVAL, type ExportSpec } from "@/lib/experiment/spec"
import { amountIn, parseQuantity } from "@/lib/units"
import { TagsInput } from "@mantine/core"

// A value stored in another unit is converted for display rather than refused.
const UNIT = "min"

// Wide enough for the unit to sit clear of the last entry.
const UNIT_WIDTH = 44

// Only the interval is edited; an existing entry keeps its columns and file list.
export function ExportAxis({
    entries,
    onChange,
}: Readonly<{ entries: ExportSpec[]; onChange: (next: ExportSpec[]) => void }>) {
    return (
        <TagsInput
            label={<FieldLabel label="Export interval" help={AXIS_HELP.exportModels} />}
            size="sm"
            value={entries.map(minutesOf)}
            onChange={(tags) => onChange(rebuild(tags, entries))}
            splitChars={[",", " "]}
            placeholder={entries.length === 0 ? "Add an interval" : undefined}
            rightSection={<UnitAdornment unit={UNIT} />}
            rightSectionWidth={UNIT_WIDTH}
            rightSectionPointerEvents="none"
        />
    )
}

function minutesOf(entry: ExportSpec): string {
    const wire = entry.exportInterval ?? DEFAULT_EXPORT_INTERVAL
    const parsed = parseQuantity("time", wire)
    // Shown as stored when unreadable; hiding it would drop the entry on the next edit.
    return parsed.status === "ok" ? String(amountIn("time", parsed.base, UNIT)) : String(wire)
}

// A tag matching an existing entry keeps it whole; matched in order, so equal intervals stay apart.
function rebuild(tags: string[], entries: ExportSpec[]): ExportSpec[] {
    const remaining = [...entries]

    return tags.flatMap((tag) => {
        const at = remaining.findIndex((entry) => minutesOf(entry) === tag)
        if (at !== -1) return remaining.splice(at, 1)
        const minutes = Number(tag)
        return Number.isFinite(minutes) && minutes > 0 ? [{ exportInterval: `${minutes} ${UNIT}` }] : []
    })
}
