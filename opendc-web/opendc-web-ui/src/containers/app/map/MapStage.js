import React from 'react'
import { useDispatch } from 'react-redux'
import { setMapDimensions, setMapPositionWithBoundsCheck, zoomInOnPosition } from '../../../actions/map'
import MapStageComponent from '../../../components/app/map/MapStageComponent'
import { useMapDimensions, useMapPosition } from '../../../store/hooks/map'

const MapStage = () => {
    const position = useMapPosition()
    const dimensions = useMapDimensions()
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
