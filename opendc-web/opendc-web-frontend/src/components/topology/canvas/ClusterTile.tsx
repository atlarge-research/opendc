"use client"

import { formatMemory, formatPower } from "@/components/format"
import { TILE_BODY, TILE_INSET, TILE_SIZE, cellOrigin } from "@/components/topology/canvas/geometry"
import type { CanvasPalette } from "@/components/topology/canvas/palette"
import type { HardwareIcons } from "@/components/topology/canvas/useHardwareIcons"
import { type PowerBudget, clusterCapacity, clusterShare } from "@/lib/topology/capacity"
import type { FloorCell } from "@/lib/topology/layout"
import { type ClusterSpec, clusterCount, clusterName } from "@/lib/topology/spec"
import type { KonvaEventObject } from "konva/lib/Node"
import { Group, Image as KonvaImage, Line, Rect, Text } from "react-konva"

const PAD = 10
const CONTENT_WIDTH = TILE_BODY - PAD * 2
const ICON = 12
const GUTTER = ICON + 7
const TEXT_X = PAD + GUTTER
const TEXT_Y = 1
const TEXT_WIDTH = CONTENT_WIDTH - GUTTER
const TITLE_Y = 10
const REPEAT_WIDTH = 24
const DIVIDER_Y = 28
const ROWS_TOP = 34
const ROW_HEIGHT = 15
const FOOTER_HEIGHT = 20
const PERCENT_WIDTH = 30
const METER_X = TEXT_X + PERCENT_WIDTH
const METER_Y = 3
const METER_WIDTH = TEXT_WIDTH - PERCENT_WIDTH
const METER_HEIGHT = 6
const METER_RADIUS = 3
const SMALL_FONT = 10
const SHADOW_COLOR = "#0b1220"

// The meter is this cluster's share of the shared supply; red when the data center is over budget.
export interface ClusterTileProps {
    cluster: ClusterSpec
    index: number
    cell: FloorCell
    supply: PowerBudget
    overBudget: boolean
    selected: boolean
    invalid: boolean
    palette: CanvasPalette
    icons: HardwareIcons
    onSelect: (index: number, additive: boolean) => void
    onOpen: (index: number) => void
    onPlace: (index: number, cell: FloorCell) => void
}

export function ClusterTile(props: ClusterTileProps) {
    const { cluster, index, cell, overBudget, selected, invalid, palette, icons } = props
    const origin = cellOrigin(cell)
    const capacity = clusterCapacity(cluster)
    const share = clusterShare(cluster, props.supply)
    const repeats = clusterCount(cluster)

    const limited = share.status === "limited"
    const usage = share.status === "limited" ? Math.min(1, share.fraction) : 0
    const powerLabel = share.status === "limited" ? `${Math.round(share.fraction * 100)}%` : formatPower(share.drawW)

    const rows = [
        { icon: icons.space, label: `${capacity.hosts} hosts` },
        { icon: icons.cpu, label: `${capacity.cores} cores` },
        { icon: icons.memory, label: formatMemory(capacity.memoryMiB) },
        ...(capacity.gpus > 0 ? [{ icon: icons.gpu, label: `${capacity.gpus} GPUs` }] : []),
    ]

    const handleDragEnd = (event: KonvaEventObject<DragEvent>) => {
        const node = event.target
        const dropped = {
            x: Math.round((node.x() - TILE_INSET) / TILE_SIZE),
            y: Math.round((node.y() - TILE_INSET) / TILE_SIZE),
        }
        node.position({ x: origin.x + TILE_INSET, y: origin.y + TILE_INSET })
        props.onPlace(index, dropped)
    }

    return (
        <Group
            x={origin.x + TILE_INSET}
            y={origin.y + TILE_INSET}
            draggable
            onDragEnd={handleDragEnd}
            onClick={(event) => props.onSelect(index, event.evt.shiftKey)}
            onTap={() => props.onSelect(index, false)}
            onDblClick={() => props.onOpen(index)}
        >
            <Rect
                width={TILE_BODY}
                height={TILE_BODY}
                cornerRadius={10}
                fill={selected ? palette.clusterSelected : palette.cluster}
                stroke={invalid ? palette.invalidBorder : selected ? palette.borderSelected : palette.border}
                strokeWidth={selected || invalid ? 1.5 : 1}
                shadowColor={SHADOW_COLOR}
                shadowOpacity={selected ? 0.16 : 0.06}
                shadowBlur={selected ? 12 : 5}
                shadowOffsetY={2}
            />

            <Text
                x={PAD}
                y={TITLE_Y}
                width={CONTENT_WIDTH - (repeats > 1 ? REPEAT_WIDTH : 0)}
                text={clusterName(cluster)}
                fontSize={12}
                fontStyle="600"
                fill={palette.title}
                wrap="none"
                ellipsis
            />
            {repeats > 1 && (
                <Text
                    x={TILE_BODY - PAD - REPEAT_WIDTH}
                    y={TITLE_Y + 1}
                    width={REPEAT_WIDTH}
                    align="right"
                    text={`x${repeats}`}
                    fontSize={SMALL_FONT}
                    fill={palette.accent}
                />
            )}

            <Line points={[PAD, DIVIDER_Y, TILE_BODY - PAD, DIVIDER_Y]} stroke={palette.divider} strokeWidth={1} />

            {rows.map((row, position) => (
                <IconRow
                    key={row.label}
                    y={ROWS_TOP + position * ROW_HEIGHT}
                    icon={row.icon}
                    label={row.label}
                    palette={palette}
                />
            ))}

            <Group y={TILE_BODY - FOOTER_HEIGHT}>
                {icons.energy && <KonvaImage image={icons.energy} x={PAD} y={0} width={ICON} height={ICON} />}
                <Text
                    x={TEXT_X}
                    y={TEXT_Y}
                    width={limited ? PERCENT_WIDTH : TEXT_WIDTH}
                    text={powerLabel}
                    fontSize={SMALL_FONT}
                    fill={overBudget ? palette.meterOver : palette.muted}
                    wrap="none"
                    ellipsis
                />
                {limited && (
                    <>
                        <Rect
                            x={METER_X}
                            y={METER_Y}
                            width={METER_WIDTH}
                            height={METER_HEIGHT}
                            cornerRadius={METER_RADIUS}
                            fill={palette.meterTrack}
                        />
                        <Rect
                            x={METER_X}
                            y={METER_Y}
                            width={METER_WIDTH * usage}
                            height={METER_HEIGHT}
                            cornerRadius={METER_RADIUS}
                            fill={overBudget ? palette.meterOver : palette.energyFill}
                        />
                    </>
                )}
            </Group>
        </Group>
    )
}

function IconRow({
    y,
    icon,
    label,
    palette,
}: {
    y: number
    icon: HTMLImageElement | undefined
    label: string
    palette: CanvasPalette
}) {
    return (
        <Group y={y}>
            {icon && <KonvaImage image={icon} x={PAD} y={0} width={ICON} height={ICON} opacity={0.7} />}
            <Text
                x={TEXT_X}
                y={TEXT_Y}
                width={TEXT_WIDTH}
                text={label}
                fontSize={11}
                fill={palette.label}
                wrap="none"
                ellipsis
            />
        </Group>
    )
}
