import PropTypes from "prop-types";
import React from "react";
import {Rect} from "react-konva";
import {ROOM_HOVER_INVALID_COLOR, ROOM_HOVER_VALID_COLOR} from "../../../util/colors";
import {TILE_SIZE_IN_PIXELS} from "../MapConstants";

const HoverTile = ({pixelX, pixelY, isValid, scale, onClick}) => (
    <Rect
        x={pixelX}
        y={pixelY}
        scaleX={scale}
        scaleY={scale}
        width={TILE_SIZE_IN_PIXELS}
        height={TILE_SIZE_IN_PIXELS}
        fill={isValid ? ROOM_HOVER_VALID_COLOR : ROOM_HOVER_INVALID_COLOR}
        onClick={onClick}
    />
);

HoverTile.propTypes = {
    pixelX: PropTypes.number.isRequired,
    pixelY: PropTypes.number.isRequired,
    isValid: PropTypes.bool.isRequired,
    onClick: PropTypes.func.isRequired,
};

export default HoverTile;
