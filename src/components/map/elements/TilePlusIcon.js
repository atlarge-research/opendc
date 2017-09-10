import PropTypes from "prop-types";
import React from "react";
import {Group, Line} from "react-konva";
import {TILE_PLUS_COLOR} from "../../../colors/index";
import {TILE_PLUS_MARGIN_IN_PIXELS, TILE_PLUS_WIDTH_IN_PIXELS, TILE_SIZE_IN_PIXELS} from "../MapConstants";

const TilePlusIcon = ({pixelX, pixelY, scale}) => {
    const linePoints = [
        [
            pixelX + 0.5 * TILE_SIZE_IN_PIXELS * scale,
            pixelY + TILE_PLUS_MARGIN_IN_PIXELS * scale,
            pixelX + 0.5 * TILE_SIZE_IN_PIXELS * scale,
            pixelY + TILE_SIZE_IN_PIXELS * scale - TILE_PLUS_MARGIN_IN_PIXELS * scale,
        ],
        [
            pixelX + TILE_PLUS_MARGIN_IN_PIXELS * scale,
            pixelY + 0.5 * TILE_SIZE_IN_PIXELS * scale,
            pixelX + TILE_SIZE_IN_PIXELS * scale - TILE_PLUS_MARGIN_IN_PIXELS * scale,
            pixelY + 0.5 * TILE_SIZE_IN_PIXELS * scale,
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
                    strokeWidth={TILE_PLUS_WIDTH_IN_PIXELS * scale}
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
