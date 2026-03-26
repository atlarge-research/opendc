import PropTypes from 'prop-types'
import React from 'react'
import { Group, Line } from 'react-konva'
import { TILE_PLUS_COLOR } from '../../../../util/colors'
import { TILE_PLUS_MARGIN_IN_PIXELS, TILE_PLUS_WIDTH_IN_PIXELS, TILE_SIZE_IN_PIXELS } from '../MapConstants'

function TilePlusIcon({ x, y, scale = 1 }) {
    const linePoints = [
        [
            x + 0.5 * TILE_SIZE_IN_PIXELS * scale,
            y + TILE_PLUS_MARGIN_IN_PIXELS * scale,
            x + 0.5 * TILE_SIZE_IN_PIXELS * scale,
            y + TILE_SIZE_IN_PIXELS * scale - TILE_PLUS_MARGIN_IN_PIXELS * scale,
        ],
        [
            x + TILE_PLUS_MARGIN_IN_PIXELS * scale,
            y + 0.5 * TILE_SIZE_IN_PIXELS * scale,
            x + TILE_SIZE_IN_PIXELS * scale - TILE_PLUS_MARGIN_IN_PIXELS * scale,
            y + 0.5 * TILE_SIZE_IN_PIXELS * scale,
        ],
    ]
    return (
        <Group>
            {linePoints.map((points, index) => (
                <Line
                    key={index}
                    points={points}
                    lineCap="round"
                    stroke={TILE_PLUS_COLOR}
                    strokeWidth={TILE_PLUS_WIDTH_IN_PIXELS * scale}
                    listening={false}
                />
            ))}
        </Group>
    )
}

TilePlusIcon.propTypes = {
    x: PropTypes.number,
    y: PropTypes.number,
    scale: PropTypes.number,
}

export default TilePlusIcon
