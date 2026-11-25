import type { FloorCell, FloorLayout } from "@/lib/topology/layout"

export const TILE_SIZE = 132
export const TILE_INSET = 6
export const TILE_BODY = TILE_SIZE - TILE_INSET * 2
export const MIN_SCALE = 0.35
export const MAX_SCALE = 2.5
export const SCALE_STEP = 1.15

export interface Point {
    x: number
    y: number
}

export function cellOrigin(cell: FloorCell): Point {
    return { x: cell.x * TILE_SIZE, y: cell.y * TILE_SIZE }
}

export function cellAtPoint(point: Point): FloorCell {
    return { x: Math.floor(point.x / TILE_SIZE), y: Math.floor(point.y / TILE_SIZE) }
}

export function floorPixelSize(layout: FloorLayout): { width: number; height: number } {
    return { width: layout.size.width * TILE_SIZE, height: layout.size.height * TILE_SIZE }
}

export function withinFloor(cell: FloorCell, layout: FloorLayout): boolean {
    return cell.x >= 0 && cell.y >= 0 && cell.x < layout.size.width && cell.y < layout.size.height
}

export function clampScale(scale: number): number {
    return Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale))
}

const FIT_MARGIN = 40
const MIN_FIT_SCALE = 0.8
const MAX_FIT_SCALE = 1.3
const EMPTY_VIEW_CELLS = { width: 5, height: 4 }

export interface FloorView {
    scale: number
    origin: Point
}

export function fitToContent(layout: FloorLayout, viewport: { width: number; height: number }): FloorView {
    if (viewport.width === 0 || viewport.height === 0) return { scale: 1, origin: { x: FIT_MARGIN, y: FIT_MARGIN } }

    const cells = layout.cells
    const bounds =
        cells.length === 0
            ? { minX: 0, minY: 0, maxX: EMPTY_VIEW_CELLS.width - 1, maxY: EMPTY_VIEW_CELLS.height - 1 }
            : {
                  minX: Math.min(...cells.map((cell) => cell.x)),
                  minY: Math.min(...cells.map((cell) => cell.y)),
                  maxX: Math.max(...cells.map((cell) => cell.x)),
                  maxY: Math.max(...cells.map((cell) => cell.y)),
              }

    const contentWidth = (bounds.maxX - bounds.minX + 1) * TILE_SIZE
    const contentHeight = (bounds.maxY - bounds.minY + 1) * TILE_SIZE
    const available = Math.min(
        (viewport.width - FIT_MARGIN * 2) / contentWidth,
        (viewport.height - FIT_MARGIN * 2) / contentHeight,
    )
    const scale = Math.min(MAX_FIT_SCALE, Math.max(MIN_FIT_SCALE, available))

    return {
        scale,
        origin: {
            x: (viewport.width - contentWidth * scale) / 2 - bounds.minX * TILE_SIZE * scale,
            y: (viewport.height - contentHeight * scale) / 2 - bounds.minY * TILE_SIZE * scale,
        },
    }
}
