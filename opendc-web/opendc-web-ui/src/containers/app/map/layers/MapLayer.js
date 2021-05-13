import React from 'react'
import MapLayerComponent from '../../../../components/app/map/layers/MapLayerComponent'
import { useMapPosition, useMapScale } from '../../../../store/hooks/map'

const MapLayer = (props) => {
    const position = useMapPosition()
    const scale = useMapScale()
    return <MapLayerComponent {...props} mapPosition={position} mapScale={scale} />
}

export default MapLayer
