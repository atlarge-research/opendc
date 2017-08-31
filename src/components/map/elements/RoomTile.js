import React from "react";
import {Rect} from "react-konva";
import {ROOM_DEFAULT_COLOR, ROOM_IN_CONSTRUCTION_COLOR} from "../../../colors/index";
import Shapes from "../../../shapes/index";
import {TILE_SIZE_IN_PIXELS} from "../MapConstants";

const RoomTile = ({tile, newTile}) => (
    <Rect
        x={tile.positionX * TILE_SIZE_IN_PIXELS}
        y={tile.positionY * TILE_SIZE_IN_PIXELS}
        width={TILE_SIZE_IN_PIXELS}
        height={TILE_SIZE_IN_PIXELS}
        fill={newTile ? ROOM_IN_CONSTRUCTION_COLOR : ROOM_DEFAULT_COLOR}
    />
);

RoomTile.propTypes = {
    tile: Shapes.Tile,
};

export default RoomTile;
