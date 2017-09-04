import React from "react";
import {Group} from "react-konva";
import {RACK_BACKGROUND_COLOR} from "../../../colors/index";
import Shapes from "../../../shapes/index";
import TileObject from "../elements/TileObject";

const RackGroup = ({tile}) => (
    <Group>
        <TileObject positionX={tile.positionX} positionY={tile.positionY} color={RACK_BACKGROUND_COLOR}/>
    </Group>
);

RackGroup.propTypes = {
    tile: Shapes.Tile,
};

export default RackGroup;
