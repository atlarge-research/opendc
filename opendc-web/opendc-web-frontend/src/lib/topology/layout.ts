import type { TopologySpec } from "@/lib/topology/spec"
import { z } from "zod"

export const LAYOUT_VERSION = 2

export const DEFAULT_FLOOR_WIDTH = 20
export const DEFAULT_FLOOR_HEIGHT = 14

export const floorCellSchema = z.object({ x: z.number().int().min(0), y: z.number().int().min(0) })

export const floorLayoutSchema = z.object({
    size: z.object({ width: z.number().int().positive(), height: z.number().int().positive() }),
    cells: z.array(floorCellSchema),
})

/**
 * One floor per data center, lined up with `datacenters` by index the way cells line up with
 * clusters. Names are not unique, so they cannot be the keys.
 */
export const floorPlanSchema = z.object({
    version: z.literal(LAYOUT_VERSION),
    floors: z.array(floorLayoutSchema),
})

export type FloorCell = z.infer<typeof floorCellSchema>
export type FloorLayout = z.infer<typeof floorLayoutSchema>
export type FloorPlan = z.infer<typeof floorPlanSchema>

export type CellOccupant = { status: "empty" } | { status: "occupied"; cluster: number }

type FloorSize = FloorLayout["size"]

/**
 * The server keeps a plan only once someone has arranged one, and keeps it opaque, so a topology
 * that was never opened, came from the CLI or was saved by an older editor is laid out afresh.
 */
export function floorPlanOf(stored: unknown, topology: TopologySpec): FloorPlan {
    const parsed = floorPlanSchema.safeParse(stored)
    return parsed.success ? reconcilePlan(parsed.data, topology) : autoPlan(topology)
}

export function autoPlan(topology: TopologySpec): FloorPlan {
    return reconcilePlan({ version: LAYOUT_VERSION, floors: [] }, topology)
}

/** Trims or adds floors to match the data centers, and reconciles each with its clusters. */
export function reconcilePlan(plan: FloorPlan, topology: TopologySpec): FloorPlan {
    return {
        version: LAYOUT_VERSION,
        floors: topology.datacenters.map((dataCenter, index) =>
            reconcileFloor(plan.floors[index] ?? emptyFloor(), dataCenter.clusters.length),
        ),
    }
}

export function emptyFloor(): FloorLayout {
    return { size: { width: DEFAULT_FLOOR_WIDTH, height: DEFAULT_FLOOR_HEIGHT }, cells: [] }
}

/** Keeps every cell still valid, and gives each cluster without one the next free cell. */
export function reconcileFloor(floor: FloorLayout, clusters: number): FloorLayout {
    const size = grownTo(floor.size, clusters)
    const taken = new Set<string>()
    const kept: Array<FloorCell | undefined> = []

    for (let index = 0; index < clusters; index++) {
        const cell = floor.cells[index]
        if (cell && withinBounds(cell, size) && !taken.has(key(cell))) {
            taken.add(key(cell))
            kept.push(cell)
        } else {
            kept.push(undefined)
        }
    }

    let cursor = 0
    return {
        size,
        cells: kept.map((cell) => {
            if (cell) return cell
            const next = scanFree(size, taken, cursor)
            cursor = next.cursor + 1
            taken.add(key(next.cell))
            return next.cell
        }),
    }
}

export function clusterAt(floor: FloorLayout, cell: FloorCell): CellOccupant {
    const index = floor.cells.findIndex((candidate) => candidate.x === cell.x && candidate.y === cell.y)
    return index === -1 ? { status: "empty" } : { status: "occupied", cluster: index }
}

/** Puts a cluster on a cell, swapping with whatever was there. */
export function placeOnFloor(floor: FloorLayout, cluster: number, cell: FloorCell): FloorLayout {
    const current = floor.cells[cluster]
    if (!current || !withinBounds(cell, floor.size)) return floor
    const occupant = clusterAt(floor, cell)
    const cells = floor.cells.map((existing, index) => {
        if (index === cluster) return cell
        if (occupant.status === "occupied" && index === occupant.cluster) return current
        return existing
    })
    return { ...floor, cells }
}

/** The first free cell after `near`, growing the floor by a row when it is full. */
export function reserveCell(floor: FloorLayout, near: FloorCell): { floor: FloorLayout; cell: FloorCell } {
    return claimFrom(floor, Math.max(0, near.y * floor.size.width + near.x + 1))
}

/** The first free cell of the floor, growing it by a row when it is full. */
export function freeCell(floor: FloorLayout): { floor: FloorLayout; cell: FloorCell } {
    return claimFrom(floor, 0)
}

export function insertCell(floor: FloorLayout, cluster: number, cell: FloorCell): FloorLayout {
    const cells = [...floor.cells]
    cells.splice(cluster, 0, cell)
    return { ...floor, cells }
}

function claimFrom(floor: FloorLayout, start: number): { floor: FloorLayout; cell: FloorCell } {
    const taken = new Set(floor.cells.map(key))
    const scan = scanFree(floor.size, taken, start)
    if (withinBounds(scan.cell, floor.size)) return { floor, cell: scan.cell }
    const size = { width: floor.size.width, height: floor.size.height + 1 }
    return { floor: { ...floor, size }, cell: scanFree(size, taken, 0).cell }
}

function scanFree(size: FloorSize, taken: Set<string>, from: number): { cell: FloorCell; cursor: number } {
    const total = size.width * size.height
    for (let cursor = from; cursor < total; cursor++) {
        const cell = { x: cursor % size.width, y: Math.floor(cursor / size.width) }
        if (!taken.has(key(cell))) return { cell, cursor }
    }
    for (let cursor = 0; cursor < from && cursor < total; cursor++) {
        const cell = { x: cursor % size.width, y: Math.floor(cursor / size.width) }
        if (!taken.has(key(cell))) return { cell, cursor }
    }
    return { cell: { x: 0, y: size.height }, cursor: total }
}

function grownTo(size: FloorSize, clusters: number): FloorSize {
    const width = Math.max(size.width, 1)
    const needed = Math.ceil(clusters / width)
    return { width, height: Math.max(size.height, needed) }
}

function withinBounds(cell: FloorCell, size: FloorSize): boolean {
    return cell.x >= 0 && cell.y >= 0 && cell.x < size.width && cell.y < size.height
}

function key(cell: FloorCell): string {
    return `${cell.x},${cell.y}`
}
