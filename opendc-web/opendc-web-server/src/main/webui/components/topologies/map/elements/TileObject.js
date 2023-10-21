import PropTypes from 'prop-types'
import React from 'react'
import { Rect } from 'react-konva'
import { OBJECT_BORDER_COLOR } from '../../../../util/colors'
import { OBJECT_BORDER_WIDTH_IN_PIXELS, OBJECT_MARGIN_IN_PIXELS, TILE_SIZE_IN_PIXELS } from '../MapConstants'

function TileObject({ positionX, positionY, color }) {
    return (
        <Rect
            x={positionX * TILE_SIZE_IN_PIXELS + OBJECT_MARGIN_IN_PIXELS}
            y={positionY * TILE_SIZE_IN_PIXELS + OBJECT_MARGIN_IN_PIXELS}
            width={TILE_SIZE_IN_PIXELS - OBJECT_MARGIN_IN_PIXELS * 2}
            height={TILE_SIZE_IN_PIXELS - OBJECT_MARGIN_IN_PIXELS * 2}
            fill={color}
            stroke={OBJECT_BORDER_COLOR}
            strokeWidth={OBJECT_BORDER_WIDTH_IN_PIXELS}
        />
    )
}

TileObject.propTypes = {
    positionX: PropTypes.number.isRequired,
    positionY: PropTypes.number.isRequired,
    color: PropTypes.string.isRequired,
}

export default TileObject
