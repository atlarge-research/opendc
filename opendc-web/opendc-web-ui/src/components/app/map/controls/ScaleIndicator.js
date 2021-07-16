import PropTypes from 'prop-types'
import React from 'react'
import { TILE_SIZE_IN_METERS, TILE_SIZE_IN_PIXELS } from '../MapConstants'
import { scaleIndicator } from './ScaleIndicator.module.scss'

const ScaleIndicator = ({ scale }) => (
    <div className={scaleIndicator} style={{ width: TILE_SIZE_IN_PIXELS * scale }}>
        {TILE_SIZE_IN_METERS}m
    </div>
)

ScaleIndicator.propTypes = {
    scale: PropTypes.number.isRequired,
}

export default ScaleIndicator
