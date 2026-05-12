import PropTypes from 'prop-types'
import React from 'react'
import { Group, Line, Rect } from 'react-konva'
import { GRID_COLOR } from '../../../../util/colors'
import { GRID_LINE_WIDTH_IN_PIXELS, TILE_SIZE_IN_PIXELS } from '../MapConstants'

function GridGroup({ width, height, x = 0, y = 0 }) {
    if (!width || !height) {
        return <Group />
    }

    const offsetX = x * TILE_SIZE_IN_PIXELS
    const offsetY = y * TILE_SIZE_IN_PIXELS
    const widthPx = width * TILE_SIZE_IN_PIXELS
    const heightPx = height * TILE_SIZE_IN_PIXELS

    const horizontalLines = Array.from({ length: height + 1 }, (_, i) => [
        offsetX, offsetY + i * TILE_SIZE_IN_PIXELS,
        offsetX + widthPx, offsetY + i * TILE_SIZE_IN_PIXELS,
    ])
    const verticalLines = Array.from({ length: width + 1 }, (_, i) => [
        offsetX + i * TILE_SIZE_IN_PIXELS, offsetY,
        offsetX + i * TILE_SIZE_IN_PIXELS, offsetY + heightPx,
    ])

    return (
        <Group>
            {horizontalLines.concat(verticalLines).map((points, index) => (
                <Line
                    key={index}
                    points={points}
                    stroke={GRID_COLOR}
                    strokeWidth={GRID_LINE_WIDTH_IN_PIXELS}
                    listening={false}
                />
            ))}
            <Rect
                x={offsetX}
                y={offsetY}
                width={widthPx}
                height={heightPx}
                stroke={GRID_COLOR}
                strokeWidth={GRID_LINE_WIDTH_IN_PIXELS * 3}
                fill={null}
                listening={false}
            />
        </Group>
    )
}

GridGroup.propTypes = {
    width: PropTypes.number,
    height: PropTypes.number,
    x: PropTypes.number,
    y: PropTypes.number,
}

export default GridGroup
