import PropTypes from 'prop-types'
import React from 'react'
import { TILE_SIZE_IN_METERS, TILE_SIZE_IN_PIXELS } from '../MapConstants'
import { scaleIndicator } from './ScaleIndicatorComponent.module.scss'

const ScaleIndicatorComponent = ({ scale }) => (
    <div className={scaleIndicator} style={{ width: TILE_SIZE_IN_PIXELS * scale }}>
        {TILE_SIZE_IN_METERS}m
    </div>
)

ScaleIndicatorComponent.propTypes = {
    scale: PropTypes.number.isRequired,
}

export default ScaleIndicatorComponent
