import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { setMapDimensions, setMapPositionWithBoundsCheck, zoomInOnPosition } from '../../../actions/map'
import MapStageComponent from '../../../components/app/map/MapStageComponent'

const MapStage = () => {
    const position = useSelector((state) => state.map.position)
    const dimensions = useSelector((state) => state.map.dimensions)
    const dispatch = useDispatch()
    const zoomInOnPositionA = (zoomIn, x, y) => dispatch(zoomInOnPosition(zoomIn, x, y))
    const setMapPositionWithBoundsCheckA = (x, y) => dispatch(setMapPositionWithBoundsCheck(x, y))
    const setMapDimensionsA = (width, height) => dispatch(setMapDimensions(width, height))

    return (
        <MapStageComponent
            mapPosition={position}
            mapDimensions={dimensions}
            zoomInOnPosition={zoomInOnPositionA}
            setMapPositionWithBoundsCheck={setMapPositionWithBoundsCheckA}
            setMapDimensions={setMapDimensionsA}
        />
    )
}

export default MapStage
