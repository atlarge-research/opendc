import React from 'react'
import { useSelector } from 'react-redux'
import MapLayerComponent from '../../../../components/app/map/layers/MapLayerComponent'

const MapLayer = (props) => {
    const { position, scale } = useSelector((state) => state.map)
    return <MapLayerComponent {...props} mapPosition={position} mapScale={scale} />
}

export default MapLayer
