import PropTypes from "prop-types";
import React from "react";
import {Group, Line} from "react-konva";
import {TILE_PLUS_COLOR} from "../../../colors/index";
import {TILE_PLUS_MARGIN_IN_PIXELS, TILE_PLUS_WIDTH_IN_PIXELS, TILE_SIZE_IN_PIXELS} from "../MapConstants";

const TilePlusIcon = ({pixelX, pixelY}) => {
    const linePoints = [
        [
            pixelX + 0.5 * TILE_SIZE_IN_PIXELS,
            pixelY + TILE_PLUS_MARGIN_IN_PIXELS,
            pixelX + 0.5 * TILE_SIZE_IN_PIXELS,
            pixelY + TILE_SIZE_IN_PIXELS - TILE_PLUS_MARGIN_IN_PIXELS,
        ],
        [
            pixelX + TILE_PLUS_MARGIN_IN_PIXELS,
            pixelY + 0.5 * TILE_SIZE_IN_PIXELS,
            pixelX + TILE_SIZE_IN_PIXELS - TILE_PLUS_MARGIN_IN_PIXELS,
            pixelY + 0.5 * TILE_SIZE_IN_PIXELS,
        ],
    ];
    return (
        <Group>
            {linePoints.map((points, index) => (
                <Line
                    key={index}
                    points={points}
                    lineCap="round"
                    stroke={TILE_PLUS_COLOR}
                    strokeWidth={TILE_PLUS_WIDTH_IN_PIXELS}
                    listening={false}
                />
            ))}
        </Group>
    )
};

TilePlusIcon.propTypes = {
    pixelX: PropTypes.number,
    pixelY: PropTypes.number,
};

export default TilePlusIcon;
