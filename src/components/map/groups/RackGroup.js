import React from "react";
import {Group} from "react-konva";
import {RACK_BACKGROUND_COLOR} from "../../../colors/index";
import RackEnergyFillContainer from "../../../containers/map/RackEnergyFillContainer";
import RackSpaceFillContainer from "../../../containers/map/RackSpaceFillContainer";
import Shapes from "../../../shapes/index";
import TileObject from "../elements/TileObject";

const RackGroup = ({tile}) => (
    <Group>
        <TileObject positionX={tile.positionX} positionY={tile.positionY} color={RACK_BACKGROUND_COLOR}/>
        <RackSpaceFillContainer tileId={tile.id} positionX={tile.positionX} positionY={tile.positionY}/>
        <RackEnergyFillContainer tileId={tile.id} positionX={tile.positionX} positionY={tile.positionY}/>
    </Group>
);

RackGroup.propTypes = {
    tile: Shapes.Tile,
};

export default RackGroup;
