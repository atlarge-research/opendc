import React from "react";
import {Group} from "react-konva";
import GrayContainer from "../../../containers/map/GrayContainer";
import TileContainer from "../../../containers/map/TileContainer";
import Shapes from "../../../shapes/index";
import {deriveWallLocations} from "../../../util/tile-calculations";
import WallSegment from "../elements/WallSegment";

const RoomGroup = ({room, interactionLevel, onClick}) => {
    return (
        <Group
            onClick={onClick}
        >
            {(() => {
                if (interactionLevel.mode === "OBJECT" && interactionLevel.roomId === room.id) {
                    return [
                        room.tiles
                            .filter(tile => tile.id !== interactionLevel.tileId)
                            .map(tile => <TileContainer key={tile.id} tile={tile}/>),
                        <GrayContainer key={-1}/>,
                        room.tiles
                            .filter(tile => tile.id === interactionLevel.tileId)
                            .map(tile => <TileContainer key={tile.id} tile={tile}/>)
                    ];
                } else {
                    return room.tiles.map(tile => (
                        <TileContainer key={tile.id} tile={tile}/>
                    ));
                }
            })()}
            {deriveWallLocations(room).map((wallSegment, index) => (
                <WallSegment key={index} wallSegment={wallSegment}/>
            ))}
        </Group>
    );
};

RoomGroup.propTypes = {
    room: Shapes.Room,
};

export default RoomGroup;
