import PropTypes from 'prop-types'
import React from 'react'
import { Group, Layer } from 'react-konva'
import Backdrop from '../elements/Backdrop'
import GridGroup from '../groups/GridGroup'
import TopologyContainer from '../TopologyContainer'

const MapLayerComponent = ({ mapPosition, mapScale }) => (
    <Layer>
        <Group x={mapPosition.x} y={mapPosition.y} scaleX={mapScale} scaleY={mapScale}>
            <Backdrop />
            <TopologyContainer />
            <GridGroup />
        </Group>
    </Layer>
)

MapLayerComponent.propTypes = {
    mapPosition: PropTypes.shape({
        x: PropTypes.number,
        y: PropTypes.number,
    }),
    mapScale: PropTypes.number,
}

export default MapLayerComponent
