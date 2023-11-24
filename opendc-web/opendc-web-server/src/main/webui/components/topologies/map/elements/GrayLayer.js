import PropTypes from 'prop-types'
import React from 'react'
import { Rect } from 'react-konva'
import { GRAYED_OUT_AREA_COLOR } from '../../../../util/colors'
import { MAP_SIZE_IN_PIXELS } from '../MapConstants'

function GrayLayer({ onClick }) {
    return (
        <Rect
            x={0}
            y={0}
            width={MAP_SIZE_IN_PIXELS}
            height={MAP_SIZE_IN_PIXELS}
            fill={GRAYED_OUT_AREA_COLOR}
            onClick={onClick}
        />
    )
}

GrayLayer.propTypes = {
    onClick: PropTypes.func,
}

export default GrayLayer
