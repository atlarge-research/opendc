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

export function formatDuration(seconds: number): string {
    if (seconds < 60) return `${Math.round(seconds)}s`
    if (seconds < 3600) return `${Math.round(seconds / 60)}m`
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.round((seconds % 3600) / 60)
    return minutes === 0 ? `${hours}h` : `${hours}h ${minutes}m`
}

export function formatSimulationBudget(seconds: number): string {
    if (seconds < 60) return countOf(Math.round(seconds), "simulation second")
    if (seconds < 3600) return countOf(Math.round(seconds / 60), "simulation minute")
    return countOf(Math.round(seconds / 3600), "simulation hour")
}

function countOf(count: number, unit: string): string {
    return `${integers.format(count)} ${unit}${count === 1 ? "" : "s"}`
}

export function formatPercent(fraction: number): string {
    return `${Math.round(fraction * 100)}%`
}
