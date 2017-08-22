import React from "react";
import {Rect} from "react-konva";
import {ROOM_DEFAULT_COLOR} from "../../../colors/index";
import Shapes from "../../../shapes/index";

const RoomTile = ({tile}) => (
    <Rect
        x={tile.positionX * TILE_SIZE_IN_PIXELS}
        y={tile.positionY * TILE_SIZE_IN_PIXELS}
        width={TILE_SIZE_IN_PIXELS}
        height={TILE_SIZE_IN_PIXELS}
        fill={ROOM_DEFAULT_COLOR}
    />
);

RoomTile.propTypes = {
    tile: Shapes.Tile,
};

export default RoomTile;
