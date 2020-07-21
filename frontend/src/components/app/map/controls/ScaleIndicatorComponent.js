import React from 'react'
import { TILE_SIZE_IN_METERS, TILE_SIZE_IN_PIXELS } from '../MapConstants'
import './ScaleIndicatorComponent.css'

const ScaleIndicatorComponent = ({ scale }) => (
    <div className="scale-indicator" style={{ width: TILE_SIZE_IN_PIXELS * scale }}>
        {TILE_SIZE_IN_METERS}m
    </div>
)

export default ScaleIndicatorComponent
