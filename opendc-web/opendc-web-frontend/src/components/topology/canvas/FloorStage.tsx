"use client"

import { ClusterTile } from "@/components/topology/canvas/ClusterTile"
import {
    SCALE_STEP,
    TILE_BODY,
    TILE_INSET,
    TILE_SIZE,
    cellAtPoint,
    clampScale,
    fitToContent,
    floorPixelSize,
    withinFloor,
} from "@/components/topology/canvas/geometry"
import { useCanvasPalette } from "@/components/topology/canvas/palette"
import { useHardwareIcons } from "@/components/topology/canvas/useHardwareIcons"
import type { PowerBudget } from "@/lib/topology/capacity"
import type { FloorCell, FloorLayout } from "@/lib/topology/layout"
import type { ClusterSpec } from "@/lib/topology/spec"
import { useElementSize } from "@mantine/hooks"
import type Konva from "konva"
import type { KonvaEventObject } from "konva/lib/Node"
import { useCallback, useEffect, useRef, useState } from "react"
import { Group, Layer, Line, Rect, Stage, Text } from "react-konva"

const DRAG_TOLERANCE = 4

export type ZoomAction = "in" | "out" | "fit"

export interface ZoomCommand {
    action: ZoomAction
    nonce: number
}

/** One data center's floor. The builder binds which data center the callbacks act on. */
export interface FloorStageProps {
    floor: FloorLayout
    clusters: ClusterSpec[]
    supply: PowerBudget
    overBudget: boolean
    selected: number[]
    broken: number[]
    zoom: ZoomCommand
    onCreate: (cell: FloorCell) => void
    onSelect: (cluster: number, additive: boolean) => void
    onClearSelection: () => void
    onPlace: (cluster: number, cell: FloorCell) => void
    onOpen: (cluster: number) => void
}

export function FloorStage(props: FloorStageProps) {
    const { floor: layout, clusters } = props
    const palette = useCanvasPalette()
    const icons = useHardwareIcons()
    const { ref, width, height } = useElementSize()
    const stageRef = useRef<Konva.Stage>(null)
    const pressedAt = useRef<{ x: number; y: number } | undefined>(undefined)
    const [scale, setScale] = useState(1)
    const [origin, setOrigin] = useState({ x: 0, y: 0 })
    const [hovered, setHovered] = useState<FloorCell | undefined>(undefined)

    const layoutRef = useRef(layout)
    layoutRef.current = layout

    const fit = useCallback((viewport: { width: number; height: number }) => {
        const view = fitToContent(layoutRef.current, viewport)
        setScale(view.scale)
        setOrigin(view.origin)
    }, [])

    useEffect(() => {
        if (width === 0 || height === 0) return
        fit({ width, height })
    }, [width, height, fit])

    // A command is carried out once. One issued before this stage mounted, on another floor, or
    // already carried out before a resize, is not replayed.
    const { action, nonce } = props.zoom
    const handled = useRef(nonce)
    useEffect(() => {
        if (nonce === handled.current) return
        handled.current = nonce
        if (action === "fit") fit({ width, height })
        else setScale((current) => clampScale(action === "in" ? current * SCALE_STEP : current / SCALE_STEP))
    }, [nonce, action, fit, width, height])

    const pointerCell = (): FloorCell | undefined => {
        const stage = stageRef.current
        const pointer = stage?.getPointerPosition()
        if (!stage || !pointer) return undefined
        const transform = stage.getAbsoluteTransform().copy().invert()
        return cellAtPoint(transform.point(pointer))
    }

    const handleMouseMove = () => {
        const cell = pointerCell()
        setHovered(cell && withinFloor(cell, layout) ? cell : undefined)
    }

    const handleStageClick = (event: KonvaEventObject<MouseEvent>) => {
        const start = pressedAt.current
        const moved =
            start !== undefined &&
            (Math.abs(event.evt.clientX - start.x) > DRAG_TOLERANCE ||
                Math.abs(event.evt.clientY - start.y) > DRAG_TOLERANCE)
        if (moved || event.target !== event.target.getStage()) return
        const cell = pointerCell()
        if (cell && withinFloor(cell, layout)) props.onCreate(cell)
        else props.onClearSelection()
    }

    const handleWheel = (event: KonvaEventObject<WheelEvent>) => {
        event.evt.preventDefault()
        setScale((current) => clampScale(event.evt.deltaY > 0 ? current / SCALE_STEP : current * SCALE_STEP))
    }

    const floor = floorPixelSize(layout)
    const occupied = new Set(layout.cells.map((cell) => `${cell.x},${cell.y}`))
    const hoverFree = hovered !== undefined && !occupied.has(`${hovered.x},${hovered.y}`)

    return (
        <div ref={ref} style={{ width: "100%", height: "100%" }}>
            <Stage
                ref={stageRef}
                width={Math.max(width, 1)}
                height={Math.max(height, 1)}
                scaleX={scale}
                scaleY={scale}
                x={origin.x}
                y={origin.y}
                draggable
                onWheel={handleWheel}
                onMouseMove={handleMouseMove}
                onMouseLeave={() => setHovered(undefined)}
                onMouseDown={(event) => {
                    pressedAt.current = { x: event.evt.clientX, y: event.evt.clientY }
                }}
                onDragEnd={(event) => {
                    if (event.target === event.target.getStage()) {
                        setOrigin({ x: event.target.x(), y: event.target.y() })
                    }
                }}
                onClick={handleStageClick}
            >
                <Layer listening={false}>
                    <Rect width={floor.width} height={floor.height} fill={palette.surface} cornerRadius={8} />
                    <FloorGrid columns={layout.size.width} rows={layout.size.height} color={palette.grid} />
                    {hoverFree && <HoverCell cell={hovered} color={palette.hover} />}
                </Layer>
                <Layer>
                    {clusters.map((cluster, index) => {
                        const cell = layout.cells[index]
                        if (!cell) return undefined
                        return (
                            <ClusterTile
                                key={`${index}-${cell.x}-${cell.y}`}
                                cluster={cluster}
                                index={index}
                                cell={cell}
                                supply={props.supply}
                                overBudget={props.overBudget}
                                selected={props.selected.includes(index)}
                                invalid={props.broken.includes(index)}
                                palette={palette}
                                icons={icons}
                                onSelect={props.onSelect}
                                onOpen={props.onOpen}
                                onPlace={props.onPlace}
                            />
                        )
                    })}
                </Layer>
            </Stage>
        </div>
    )
}

function FloorGrid({ columns, rows, color }: { columns: number; rows: number; color: string }) {
    const lines = []
    for (let column = 0; column <= columns; column++) {
        lines.push(
            <Line
                key={`v${column}`}
                points={[column * TILE_SIZE, 0, column * TILE_SIZE, rows * TILE_SIZE]}
                stroke={color}
                strokeWidth={1}
            />,
        )
    }
    for (let row = 0; row <= rows; row++) {
        lines.push(
            <Line
                key={`h${row}`}
                points={[0, row * TILE_SIZE, columns * TILE_SIZE, row * TILE_SIZE]}
                stroke={color}
                strokeWidth={1}
            />,
        )
    }
    return <Group opacity={0.35}>{lines}</Group>
}

function HoverCell({ cell, color }: { cell: FloorCell; color: string }) {
    const x = cell.x * TILE_SIZE + TILE_INSET
    const y = cell.y * TILE_SIZE + TILE_INSET
    return (
        <Group>
            <Rect
                x={x}
                y={y}
                width={TILE_BODY}
                height={TILE_BODY}
                cornerRadius={6}
                stroke={color}
                strokeWidth={2}
                dash={[6, 4]}
                opacity={0.8}
            />
            <Text
                x={x}
                y={y + TILE_BODY / 2 - 12}
                width={TILE_BODY}
                align="center"
                text="+"
                fontSize={26}
                fill={color}
                opacity={0.9}
            />
        </Group>
    )
}
