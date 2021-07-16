import React, { useEffect, useRef, useState } from 'react'
import { HotKeys } from 'react-hotkeys'
import { Stage } from 'react-konva'
import { MAP_MOVE_PIXELS_PER_EVENT } from './MapConstants'
import { Provider, useDispatch, useStore } from 'react-redux'
import useResizeObserver from 'use-resize-observer'
import { mapContainer } from './MapStage.module.scss'
import { useMapPosition } from '../../../data/map'
import { setMapDimensions, setMapPositionWithBoundsCheck, zoomInOnPosition } from '../../../redux/actions/map'
import MapLayer from './layers/MapLayer'
import RoomHoverLayer from './layers/RoomHoverLayer'
import ObjectHoverLayer from './layers/ObjectHoverLayer'

function MapStage() {
    const store = useStore()
    const dispatch = useDispatch()

    const stage = useRef(null)
    const [pos, setPos] = useState([0, 0])
    const [x, y] = pos
    const handlers = {
        MOVE_LEFT: () => moveWithDelta(MAP_MOVE_PIXELS_PER_EVENT, 0),
        MOVE_RIGHT: () => moveWithDelta(-MAP_MOVE_PIXELS_PER_EVENT, 0),
        MOVE_UP: () => moveWithDelta(0, MAP_MOVE_PIXELS_PER_EVENT),
        MOVE_DOWN: () => moveWithDelta(0, -MAP_MOVE_PIXELS_PER_EVENT),
    }
    const mapPosition = useMapPosition()
    const { ref, width = 100, height = 100 } = useResizeObserver()

    const moveWithDelta = (deltaX, deltaY) =>
        dispatch(setMapPositionWithBoundsCheck(mapPosition.x + deltaX, mapPosition.y + deltaY))
    const updateMousePosition = () => {
        if (!stage.current) {
            return
        }

        const mousePos = stage.current.getStage().getPointerPosition()
        setPos([mousePos.x, mousePos.y])
    }
    const updateScale = ({ evt }) => dispatch(zoomInOnPosition(evt.deltaY < 0, x, y))

    useEffect(() => {
        window['exportCanvasToImage'] = () => {
            const download = document.createElement('a')
            download.href = stage.current.getStage().toDataURL()
            download.download = 'opendc-canvas-export-' + Date.now() + '.png'
            download.click()
        }
    }, [stage])

    useEffect(() => dispatch(setMapDimensions(width, height)), [width, height]) // eslint-disable-line react-hooks/exhaustive-deps

    return (
        <HotKeys handlers={handlers} allowChanges={true} innerRef={ref} className={mapContainer}>
            <Stage ref={stage} width={width} height={height} onMouseMove={updateMousePosition} onWheel={updateScale} draggable>
                <Provider store={store}>
                    <MapLayer />
                    <RoomHoverLayer mouseX={x} mouseY={y} />
                    <ObjectHoverLayer mouseX={x} mouseY={y} />
                </Provider>
            </Stage>
        </HotKeys>
    )
}

export default MapStage
