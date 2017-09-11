import PropTypes from "prop-types";
import React from "react";
import {Group, Line} from "react-konva";
import {TILE_PLUS_COLOR} from "../../../colors/index";
import {TILE_PLUS_MARGIN_IN_PIXELS, TILE_PLUS_WIDTH_IN_PIXELS, TILE_SIZE_IN_PIXELS} from "../MapConstants";

const TilePlusIcon = ({pixelX, pixelY, mapScale}) => {
    const linePoints = [
        [
            pixelX + 0.5 * TILE_SIZE_IN_PIXELS * mapScale,
            pixelY + TILE_PLUS_MARGIN_IN_PIXELS * mapScale,
            pixelX + 0.5 * TILE_SIZE_IN_PIXELS * mapScale,
            pixelY + TILE_SIZE_IN_PIXELS * mapScale - TILE_PLUS_MARGIN_IN_PIXELS * mapScale,
        ],
        [
            pixelX + TILE_PLUS_MARGIN_IN_PIXELS * mapScale,
            pixelY + 0.5 * TILE_SIZE_IN_PIXELS * mapScale,
            pixelX + TILE_SIZE_IN_PIXELS * mapScale - TILE_PLUS_MARGIN_IN_PIXELS * mapScale,
            pixelY + 0.5 * TILE_SIZE_IN_PIXELS * mapScale,
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
                    strokeWidth={TILE_PLUS_WIDTH_IN_PIXELS * mapScale}
                    listening={false}
                />
            ))}
        </Group>
    )
};

TilePlusIcon.propTypes = {
    pixelX: PropTypes.number,
    pixelY: PropTypes.number,
    mapScale: PropTypes.number,
};

export default TilePlusIcon;
