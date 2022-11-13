import React, { useRef, useState, useContext } from 'react'
import PropTypes from 'prop-types'
import { useHotkeys } from 'react-hotkeys-hook'
import { Stage } from 'react-konva'
import { MAP_MAX_SCALE, MAP_MIN_SCALE, MAP_MOVE_PIXELS_PER_EVENT, MAP_SCALE_PER_EVENT } from './MapConstants'
import { ReactReduxContext } from 'react-redux'
import useResizeObserver from 'use-resize-observer'
import { mapContainer } from './MapStage.module.css'
import MapLayer from './layers/MapLayer'
import RoomHoverLayer from './layers/RoomHoverLayer'
import ObjectHoverLayer from './layers/ObjectHoverLayer'
import ScaleIndicator from './controls/ScaleIndicator'
import Toolbar from './controls/Toolbar'

function MapStage({ hotkeysRef }) {
    const reduxContext = useContext(ReactReduxContext)
    const stageRef = useRef(null)
    const { width = 500, height = 500 } = useResizeObserver({ ref: stageRef.current?.attrs?.container })
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

    useHotkeys('left, a', () => moveWithDelta(MAP_MOVE_PIXELS_PER_EVENT, 0), { element: hotkeysRef.current })
    useHotkeys('right, d', () => moveWithDelta(-MAP_MOVE_PIXELS_PER_EVENT, 0), { element: hotkeysRef.current })
    useHotkeys('up, w', () => moveWithDelta(0, MAP_MOVE_PIXELS_PER_EVENT), { element: hotkeysRef.current })
    useHotkeys('down, s', () => moveWithDelta(0, -MAP_MOVE_PIXELS_PER_EVENT), { element: hotkeysRef.current })

    return (
        <>
            <Stage
                className={mapContainer}
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
                <ReactReduxContext.Provider value={reduxContext}>
                    <MapLayer />
                    <RoomHoverLayer />
                    <ObjectHoverLayer />
                </ReactReduxContext.Provider>
            </Stage>
            <ScaleIndicator scale={scale} />
            <Toolbar onZoom={onZoomButton} onExport={onExport} />
        </>
    )
}

MapStage.propTypes = {
    hotkeysRef: PropTypes.object.isRequired,
}

export default MapStage
