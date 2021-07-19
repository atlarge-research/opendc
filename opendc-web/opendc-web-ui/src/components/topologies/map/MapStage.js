import React, { useRef, useState } from 'react'
import { HotKeys } from 'react-hotkeys'
import { Stage } from 'react-konva'
import { MAP_MAX_SCALE, MAP_MIN_SCALE, MAP_MOVE_PIXELS_PER_EVENT, MAP_SCALE_PER_EVENT } from './MapConstants'
import { Provider, useStore } from 'react-redux'
import useResizeObserver from 'use-resize-observer'
import { mapContainer } from './MapStage.module.scss'
import MapLayer from './layers/MapLayer'
import RoomHoverLayer from './layers/RoomHoverLayer'
import ObjectHoverLayer from './layers/ObjectHoverLayer'
import ScaleIndicator from './controls/ScaleIndicator'
import Toolbar from './controls/Toolbar'

function MapStage() {
    const store = useStore()
    const { ref, width = 100, height = 100 } = useResizeObserver()
    const stageRef = useRef(null)
    const [[x, y], setPos] = useState([0, 0])
    const [scale, setScale] = useState(1)

    const clampScale = (target) => Math.min(Math.max(target, MAP_MIN_SCALE), MAP_MAX_SCALE)
    const moveWithDelta = (deltaX, deltaY) => setPos(([x, y]) => [x + deltaX, y + deltaY])

    const onZoom = (e) => {
        e.evt.preventDefault()

        const stage = stageRef.current.getStage()
        const oldScale = scale

        const pointer = stage.getPointerPosition()
        const mousePointTo = {
            x: (pointer.x - x) / oldScale,
            y: (pointer.y - y) / oldScale,
        }

        const newScale = clampScale(e.evt.deltaY > 0 ? oldScale * MAP_SCALE_PER_EVENT : oldScale / MAP_SCALE_PER_EVENT)

        setScale(newScale)
        setPos([pointer.x - mousePointTo.x * newScale, pointer.y - mousePointTo.y * newScale])
    }
    const onZoomButton = (zoomIn) =>
        setScale((scale) => clampScale(zoomIn ? scale * MAP_SCALE_PER_EVENT : scale / MAP_SCALE_PER_EVENT))
    const onDragEnd = (e) => setPos([e.target.x(), e.target.y()])
    const onExport = () => {
        const download = document.createElement('a')
        download.href = stageRef.current.getStage().toDataURL()
        download.download = 'opendc-canvas-export-' + Date.now() + '.png'
        download.click()
    }

    const handlers = {
        MOVE_LEFT: () => moveWithDelta(MAP_MOVE_PIXELS_PER_EVENT, 0),
        MOVE_RIGHT: () => moveWithDelta(-MAP_MOVE_PIXELS_PER_EVENT, 0),
        MOVE_UP: () => moveWithDelta(0, MAP_MOVE_PIXELS_PER_EVENT),
        MOVE_DOWN: () => moveWithDelta(0, -MAP_MOVE_PIXELS_PER_EVENT),
    }

    return (
        <HotKeys handlers={handlers} allowChanges={true} innerRef={ref} className={mapContainer}>
            <Stage
                ref={stageRef}
                onWheel={onZoom}
                onDragEnd={onDragEnd}
                draggable
                width={width}
                height={height}
                scale={{ x: scale, y: scale }}
                x={x}
                y={y}
            >
                <Provider store={store}>
                    <MapLayer />
                    <RoomHoverLayer />
                    <ObjectHoverLayer />
                </Provider>
            </Stage>
            <ScaleIndicator scale={scale} />
            <Toolbar onZoom={onZoomButton} onExport={onExport} />
        </HotKeys>
    )
}

export default MapStage
