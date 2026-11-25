import { formatQuantity } from "@/lib/units"
import dayjs from "dayjs"
import isToday from "dayjs/plugin/isToday"
import isYesterday from "dayjs/plugin/isYesterday"

dayjs.extend(isToday)
dayjs.extend(isYesterday)

const integers = new Intl.NumberFormat("en-GB")

export function formatUpdatedAt(value: string): string {
    const date = dayjs(value)
    if (date.isToday()) return `Today, ${date.format("HH:mm")}`
    if (date.isYesterday()) return `Yesterday, ${date.format("HH:mm")}`
    return date.format("MMM D, YYYY HH:mm")
}

export function formatDateTime(value: string): string {
    return dayjs(value).format("MMM D, YYYY HH:mm")
}

export function formatCount(value: number): string {
    return integers.format(value)
}

export function formatMemory(memoryMiB: number): string {
    return formatQuantity("dataSize", memoryMiB)
}

/** File sizes arrive in bytes, while the size ladder counts from mebibytes. */
export function formatBytes(bytes: number): string {
    return formatQuantity("dataSize", bytes / 1024 ** 2)
}

export function formatPower(watts: number): string {
    return formatQuantity("power", watts)
}

/** Whole seconds, then minutes, then hours; a value that rounds up to the next unit is shown in it. */
export function formatDuration(seconds: number): string {
    const rounded = Math.round(seconds)
    if (rounded < 60) return `${rounded}s`

    const totalMinutes = Math.round(rounded / 60)
    if (totalMinutes < 60) return `${totalMinutes}m`

    const hours = Math.floor(totalMinutes / 60)
    const minutes = totalMinutes % 60
    return minutes === 0 ? `${hours}h` : `${hours}h ${minutes}m`
}

export function formatSimulationBudget(seconds: number): string {
    const rounded = Math.round(seconds)
    const minutes = Math.round(rounded / 60)
    if (rounded < 60) return countOf(rounded, "simulation second")
    if (minutes < 60) return countOf(minutes, "simulation minute")
    return countOf(Math.round(rounded / 3600), "simulation hour")
}

/** A count with its noun, pluralised: "1 core", "2 cores". */
export function countOf(count: number, noun: string): string {
    return `${integers.format(count)} ${noun}${count === 1 ? "" : "s"}`
}

export function formatPercent(fraction: number): string {
    return `${Math.round(fraction * 100)}%`
}
