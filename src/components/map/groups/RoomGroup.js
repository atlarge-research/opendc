import React from "react";
import {Group} from "react-konva";
import GrayContainer from "../../../containers/map/GrayContainer";
import TileContainer from "../../../containers/map/TileContainer";
import WallContainer from "../../../containers/map/WallContainer";
import Shapes from "../../../shapes/index";

const RoomGroup = ({room, interactionLevel, currentRoomInConstruction, onClick}) => {
    if (currentRoomInConstruction === room.id) {
        return (
            <Group onClick={onClick}>
                {room.tileIds.map(tileId => (
                    <TileContainer key={tileId} tileId={tileId} newTile={true}/>
                ))}
            </Group>
        );
    }

    return (
        <Group onClick={onClick}>
            {(() => {
                if (interactionLevel.mode === "OBJECT" && interactionLevel.roomId === room.id) {
                    return [
                        room.tileIds
                            .filter(tileId => tileId !== interactionLevel.tileId)
                            .map(tileId => <TileContainer key={tileId} tileId={tileId}/>),
                        <GrayContainer key={-1}/>,
                        room.tileIds
                            .filter(tileId => tileId === interactionLevel.tileId)
                            .map(tileId => <TileContainer key={tileId} tileId={tileId}/>)
                    ];
                } else {
                    return room.tileIds.map(tileId => (
                        <TileContainer key={tileId} tileId={tileId}/>
                    ));
                }
            })()}
            <WallContainer roomId={room.id}/>
        </Group>
    );
};

RoomGroup.propTypes = {
    room: Shapes.Room,
};

export default RoomGroup;
