import React from 'react';
import {Layer} from "react-konva";
import HoverTile from "../elements/HoverTile";
import {TILE_SIZE_IN_PIXELS} from "../MapConstants";

const HoverTileLayerComponent = ({mainGroupX, mainGroupY, mouseX, mouseY, currentRoomInConstruction, isValid, onClick}) => {
    if (currentRoomInConstruction === -1) {
        return <Layer/>
    }

    const positionX = Math.floor((mouseX - mainGroupX) / TILE_SIZE_IN_PIXELS);
    const positionY = Math.floor((mouseY - mainGroupY) / TILE_SIZE_IN_PIXELS);
    const pixelX = positionX * TILE_SIZE_IN_PIXELS + mainGroupX;
    const pixelY = positionY * TILE_SIZE_IN_PIXELS + mainGroupY;
    const validity = isValid(positionX, positionY);

    return (
        <Layer>
            <HoverTile
                pixelX={pixelX} pixelY={pixelY}
                isValid={validity} onClick={() => onClick(positionX, positionY)}
            />
        </Layer>
    );
};

export default HoverTileLayerComponent;
