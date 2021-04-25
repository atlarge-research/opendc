import React from 'react'
import { Group, Layer } from 'react-konva'
import TopologyContainer from '../../../../containers/app/map/TopologyContainer'
import Backdrop from '../elements/Backdrop'
import GridGroup from '../groups/GridGroup'

const MapLayerComponent = ({ mapPosition, mapScale }) => (
    <Layer>
        <Group x={mapPosition.x} y={mapPosition.y} scaleX={mapScale} scaleY={mapScale}>
            <Backdrop />
            <TopologyContainer />
            <GridGroup />
        </Group>
    </Layer>
)

export default MapLayerComponent
