import PropTypes from "prop-types";
import React from "react";
import {Rect} from "react-konva";
import {OBJECT_BORDER_COLOR, ROOM_DEFAULT_COLOR} from "../../../colors/index";
import Shapes from "../../../shapes/index";
import {OBJECT_BORDER_WIDTH_IN_PIXELS, OBJECT_MARGIN_IN_PIXELS, TILE_SIZE_IN_PIXELS} from "../MapConstants";

const TileObject = ({tile, color}) => (
    <Rect
        x={tile.positionX * TILE_SIZE_IN_PIXELS + OBJECT_MARGIN_IN_PIXELS}
        y={tile.positionY * TILE_SIZE_IN_PIXELS + OBJECT_MARGIN_IN_PIXELS}
        width={TILE_SIZE_IN_PIXELS - OBJECT_MARGIN_IN_PIXELS * 2}
        height={TILE_SIZE_IN_PIXELS - OBJECT_MARGIN_IN_PIXELS * 2}
        fill={ROOM_DEFAULT_COLOR}
        stroke={OBJECT_BORDER_COLOR}
        strokeWidth={OBJECT_BORDER_WIDTH_IN_PIXELS}
    />
);

TileObject.propTypes = {
    tile: Shapes.Tile,
    color: PropTypes.string.isRequired,
};

export default TileObject;
