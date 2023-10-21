import PropTypes from 'prop-types'
import React from 'react'
import { Group, Rect } from 'react-konva'
import {
    RACK_ENERGY_BAR_BACKGROUND_COLOR,
    RACK_ENERGY_BAR_FILL_COLOR,
    RACK_SPACE_BAR_BACKGROUND_COLOR,
    RACK_SPACE_BAR_FILL_COLOR,
} from '../../../../util/colors'
import {
    OBJECT_BORDER_WIDTH_IN_PIXELS,
    OBJECT_MARGIN_IN_PIXELS,
    RACK_FILL_ICON_OPACITY,
    RACK_FILL_ICON_WIDTH,
    TILE_SIZE_IN_PIXELS,
} from '../MapConstants'
import ImageComponent from './ImageComponent'

function RackFillBar({ positionX, positionY, type, fillFraction }) {
    const halfOfObjectBorderWidth = OBJECT_BORDER_WIDTH_IN_PIXELS / 2
    const x =
        positionX * TILE_SIZE_IN_PIXELS +
        OBJECT_MARGIN_IN_PIXELS +
        (type === 'space' ? halfOfObjectBorderWidth : 0.5 * (TILE_SIZE_IN_PIXELS - 2 * OBJECT_MARGIN_IN_PIXELS))
    const startY = positionY * TILE_SIZE_IN_PIXELS + OBJECT_MARGIN_IN_PIXELS + halfOfObjectBorderWidth
    const width = 0.5 * (TILE_SIZE_IN_PIXELS - OBJECT_MARGIN_IN_PIXELS * 2) - halfOfObjectBorderWidth
    const fullHeight = TILE_SIZE_IN_PIXELS - OBJECT_MARGIN_IN_PIXELS * 2 - OBJECT_BORDER_WIDTH_IN_PIXELS

    const fractionHeight = fillFraction * fullHeight
    const fractionY =
        (positionY + 1) * TILE_SIZE_IN_PIXELS - OBJECT_MARGIN_IN_PIXELS - halfOfObjectBorderWidth - fractionHeight

    return (
        <Group>
            <Rect
                x={x}
                y={startY}
                width={width}
                height={fullHeight}
                fill={type === 'space' ? RACK_SPACE_BAR_BACKGROUND_COLOR : RACK_ENERGY_BAR_BACKGROUND_COLOR}
            />
            <Rect
                x={x}
                y={fractionY}
                width={width}
                height={fractionHeight}
                fill={type === 'space' ? RACK_SPACE_BAR_FILL_COLOR : RACK_ENERGY_BAR_FILL_COLOR}
            />
            <ImageComponent
                src={'/img/topology/rack-' + type + '-icon.png'}
                x={x + width * 0.5 - RACK_FILL_ICON_WIDTH * 0.5}
                y={startY + fullHeight * 0.5 - RACK_FILL_ICON_WIDTH * 0.5}
                width={RACK_FILL_ICON_WIDTH}
                height={RACK_FILL_ICON_WIDTH}
                opacity={RACK_FILL_ICON_OPACITY}
            />
        </Group>
    )
}

RackFillBar.propTypes = {
    positionX: PropTypes.number.isRequired,
    positionY: PropTypes.number.isRequired,
    type: PropTypes.string.isRequired,
    fillFraction: PropTypes.number.isRequired,
}

export default RackFillBar
