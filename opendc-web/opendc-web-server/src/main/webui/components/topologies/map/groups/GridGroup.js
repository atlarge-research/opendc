import React from 'react'
import { Group, Line } from 'react-konva'
import { GRID_COLOR } from '../../../../util/colors'
import { GRID_LINE_WIDTH_IN_PIXELS, MAP_SIZE, MAP_SIZE_IN_PIXELS, TILE_SIZE_IN_PIXELS } from '../MapConstants'

const MAP_COORDINATE_ENTRIES = Array.from(new Array(MAP_SIZE), (x, i) => i)
const HORIZONTAL_POINT_PAIRS = MAP_COORDINATE_ENTRIES.map((index) => [
    0,
    index * TILE_SIZE_IN_PIXELS,
    MAP_SIZE_IN_PIXELS,
    index * TILE_SIZE_IN_PIXELS,
])
const VERTICAL_POINT_PAIRS = MAP_COORDINATE_ENTRIES.map((index) => [
    index * TILE_SIZE_IN_PIXELS,
    0,
    index * TILE_SIZE_IN_PIXELS,
    MAP_SIZE_IN_PIXELS,
])

function GridGroup() {
    return (
        <Group>
            {HORIZONTAL_POINT_PAIRS.concat(VERTICAL_POINT_PAIRS).map((points, index) => (
                <Line
                    key={index}
                    points={points}
                    stroke={GRID_COLOR}
                    strokeWidth={GRID_LINE_WIDTH_IN_PIXELS}
                    listening={false}
                />
            ))}
        </Group>
    )
}

export default GridGroup
