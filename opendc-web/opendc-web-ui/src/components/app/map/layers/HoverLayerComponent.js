import PropTypes from 'prop-types'
import React, { useEffect, useState } from 'react'
import { Layer } from 'react-konva'
import HoverTile from '../elements/HoverTile'
import { TILE_SIZE_IN_PIXELS } from '../MapConstants'

function HoverLayerComponent({ mouseX, mouseY, mapPosition, mapScale, isEnabled, isValid, onClick, children }) {
    const [pos, setPos] = useState([-1, -1])
    const [x, y] = pos
    const [valid, setValid] = useState(false)

    useEffect(() => {
        if (!isEnabled()) {
            return
        }

        const positionX = Math.floor((mouseX - mapPosition.x) / (mapScale * TILE_SIZE_IN_PIXELS))
        const positionY = Math.floor((mouseY - mapPosition.y) / (mapScale * TILE_SIZE_IN_PIXELS))

        if (positionX !== x || positionY !== y) {
            setPos([positionX, positionY])
            setValid(isValid(positionX, positionY))
        }
    }, [mouseX, mouseY, mapPosition, mapScale])

    if (!isEnabled()) {
        return <Layer />
    }

    const pixelX = mapScale * x * TILE_SIZE_IN_PIXELS + mapPosition.x
    const pixelY = mapScale * y * TILE_SIZE_IN_PIXELS + mapPosition.y

    return (
        <Layer opacity={0.6}>
            <HoverTile
                pixelX={pixelX}
                pixelY={pixelY}
                scale={mapScale}
                isValid={valid}
                onClick={() => (valid ? onClick(x, y) : undefined)}
            />
            {children
                ? React.cloneElement(children, {
                      pixelX,
                      pixelY,
                      scale: mapScale,
                  })
                : undefined}
        </Layer>
    )
}

HoverLayerComponent.propTypes = {
    mouseX: PropTypes.number.isRequired,
    mouseY: PropTypes.number.isRequired,
    mapPosition: PropTypes.object.isRequired,
    mapScale: PropTypes.number.isRequired,
    isEnabled: PropTypes.func.isRequired,
    isValid: PropTypes.func.isRequired,
    onClick: PropTypes.func.isRequired,
}

export default HoverLayerComponent
