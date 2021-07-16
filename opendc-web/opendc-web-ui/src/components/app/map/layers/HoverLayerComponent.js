import PropTypes from 'prop-types'
import React, { useMemo, useState } from 'react'
import { Layer } from 'react-konva/lib/ReactKonva'
import HoverTile from '../elements/HoverTile'
import { TILE_SIZE_IN_PIXELS } from '../MapConstants'
import { useEffectRef } from '../../../../util/effect-ref'

function HoverLayerComponent({ isEnabled, isValid, onClick, children }) {
    const [[mouseWorldX, mouseWorldY], setPos] = useState([0, 0])

    const layerRef = useEffectRef((layer) => {
        if (!layer) {
            return
        }

        const stage = layer.getStage()

        // Transform used to convert mouse coordinates to world coordinates
        const transform = stage.getAbsoluteTransform().copy()
        transform.invert()

        stage.on('mousemove.hover', () => {
            const { x, y } = transform.point(stage.getPointerPosition())
            setPos([x, y])
        })
        return () => stage.off('mousemove.hover')
    })

    const gridX = Math.floor(mouseWorldX / TILE_SIZE_IN_PIXELS)
    const gridY = Math.floor(mouseWorldY / TILE_SIZE_IN_PIXELS)
    const valid = useMemo(() => isEnabled && isValid(gridX, gridY), [isEnabled, isValid, gridX, gridY])

    if (!isEnabled) {
        return <Layer />
    }

    const x = gridX * TILE_SIZE_IN_PIXELS
    const y = gridY * TILE_SIZE_IN_PIXELS

    return (
        <Layer opacity={0.6} ref={layerRef}>
            <HoverTile x={x} y={y} isValid={valid} onClick={() => (valid ? onClick(gridX, gridY) : undefined)} />
            {children ? React.cloneElement(children, { x, y, scale: 1 }) : undefined}
        </Layer>
    )
}

HoverLayerComponent.propTypes = {
    isEnabled: PropTypes.bool.isRequired,
    isValid: PropTypes.func.isRequired,
    onClick: PropTypes.func.isRequired,
    children: PropTypes.node,
}

export default HoverLayerComponent
