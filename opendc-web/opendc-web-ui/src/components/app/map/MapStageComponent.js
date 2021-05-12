import React, { useEffect, useMemo, useRef, useState } from 'react'
import { HotKeys } from 'react-hotkeys'
import { Stage } from 'react-konva'
import MapLayer from '../../../containers/app/map/layers/MapLayer'
import ObjectHoverLayer from '../../../containers/app/map/layers/ObjectHoverLayer'
import RoomHoverLayer from '../../../containers/app/map/layers/RoomHoverLayer'
import { NAVBAR_HEIGHT } from '../../navigation/Navbar'
import { MAP_MOVE_PIXELS_PER_EVENT } from './MapConstants'
import { Provider, useStore } from 'react-redux'

function MapStageComponent({
    mapDimensions,
    mapPosition,
    setMapDimensions,
    setMapPositionWithBoundsCheck,
    zoomInOnPosition,
}) {
    const [pos, setPos] = useState([0, 0])
    const stage = useRef(null)
    const [x, y] = pos
    const handlers = {
        MOVE_LEFT: () => moveWithDelta(MAP_MOVE_PIXELS_PER_EVENT, 0),
        MOVE_RIGHT: () => moveWithDelta(-MAP_MOVE_PIXELS_PER_EVENT, 0),
        MOVE_UP: () => moveWithDelta(0, MAP_MOVE_PIXELS_PER_EVENT),
        MOVE_DOWN: () => moveWithDelta(0, -MAP_MOVE_PIXELS_PER_EVENT),
    }

    const moveWithDelta = (deltaX, deltaY) =>
        setMapPositionWithBoundsCheck(mapPosition.x + deltaX, mapPosition.y + deltaY)
    const updateMousePosition = () => {
        if (!stage.current) {
            return
        }

        const mousePos = stage.current.getStage().getPointerPosition()
        setPos([mousePos.x, mousePos.y])
    }
    const updateDimensions = () => setMapDimensions(window.innerWidth, window.innerHeight - NAVBAR_HEIGHT)
    const updateScale = (e) => zoomInOnPosition(e.deltaY < 0, x, y)

    useEffect(() => {
        updateDimensions()

        window.addEventListener('resize', updateDimensions)
        window.addEventListener('wheel', updateScale)

        window['exportCanvasToImage'] = () => {
            const download = document.createElement('a')
            download.href = stage.current.getStage().toDataURL()
            download.download = 'opendc-canvas-export-' + Date.now() + '.png'
            download.click()
        }

        return () => {
            window.removeEventListener('resize', updateDimensions)
            window.removeEventListener('wheel', updateScale)
        }
    }, [])

    const store = useStore()

    return (
        <HotKeys handlers={handlers} allowChanges={true}>
            <Stage
                ref={stage}
                width={mapDimensions.width}
                height={mapDimensions.height}
                onMouseMove={updateMousePosition}
            >
                <Provider store={store}>
                    <MapLayer />
                    <RoomHoverLayer mouseX={x} mouseY={y} />
                    <ObjectHoverLayer mouseX={x} mouseY={y} />
                </Provider>
            </Stage>
        </HotKeys>
    )
}

export default MapStageComponent
