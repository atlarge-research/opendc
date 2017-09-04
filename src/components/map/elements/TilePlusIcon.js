import React from "react";
import {Group, Line} from "react-konva";
import {TILE_PLUS_COLOR} from "../../../colors/index";
import Shapes from "../../../shapes/index";
import {TILE_PLUS_WIDTH_IN_PIXELS, TILE_SIZE_IN_PIXELS} from "../MapConstants";

const TilePlusIcon = ({positionX, positionY}) => {
    const linePoints = [
        [
            (positionX + 0.5) * TILE_SIZE_IN_PIXELS,
            positionY * TILE_SIZE_IN_PIXELS + OBJECT_MARGIN_IN_PIXELS,
            (positionX + 0.5) * TILE_SIZE_IN_PIXELS,
            (positionY + 1) * TILE_SIZE_IN_PIXELS - OBJECT_MARGIN_IN_PIXELS,
        ],
        [
            positionX * TILE_SIZE_IN_PIXELS + OBJECT_MARGIN_IN_PIXELS,
            (positionY + 0.5) * TILE_SIZE_IN_PIXELS,
            (positionX + 1) * TILE_SIZE_IN_PIXELS - OBJECT_MARGIN_IN_PIXELS,
            (positionY + 0.5) * TILE_SIZE_IN_PIXELS,
        ],
    ];
    return (
        <Group>
            {linePoints.map(points => (
                <Line
                    points={points}
                    lineCap="round"
                    stroke={TILE_PLUS_COLOR}
                    strokeWidth={TILE_PLUS_WIDTH_IN_PIXELS}
                />
            ))}
        </Group>
    )
};

TilePlusIcon.propTypes = {
    wallSegment: Shapes.WallSegment,
};

export default TilePlusIcon;
