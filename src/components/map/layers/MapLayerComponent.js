import React from 'react';
import {Group, Layer} from "react-konva";
import DatacenterContainer from "../../../containers/map/DatacenterContainer";
import Backdrop from "../elements/Backdrop";
import GridGroup from "../groups/GridGroup";

const MapLayerComponent = ({mapPosition, mapScale}) => (
    <Layer>
        <Group x={mapPosition.x} y={mapPosition.y} scaleX={mapScale} scaleY={mapScale}>
            <Backdrop/>
            <DatacenterContainer/>
            <GridGroup/>
        </Group>
    </Layer>
);

export default MapLayerComponent;
