import PropTypes from 'prop-types'
import React from 'react'
import { Rect } from 'react-konva'
import { ROOM_HOVER_INVALID_COLOR, ROOM_HOVER_VALID_COLOR } from '../../../../util/colors'
import { TILE_SIZE_IN_PIXELS } from '../MapConstants'

function HoverTile({ x, y, isValid, scale = 1, onClick }) {
    return (
        <Rect
            x={x}
            y={y}
            scaleX={scale}
            scaleY={scale}
            width={TILE_SIZE_IN_PIXELS}
            height={TILE_SIZE_IN_PIXELS}
            fill={isValid ? ROOM_HOVER_VALID_COLOR : ROOM_HOVER_INVALID_COLOR}
            onClick={onClick}
        />
    )
}

HoverTile.propTypes = {
    x: PropTypes.number.isRequired,
    y: PropTypes.number.isRequired,
    isValid: PropTypes.bool.isRequired,
    scale: PropTypes.number,
    onClick: PropTypes.func.isRequired,
}

export default HoverTile
