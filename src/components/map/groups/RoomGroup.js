import React from "react";
import {Group} from "react-konva";
import TileGroup from "./TileGroup";

const RoomGroup = ({room}) => (
    <Group>
        {room.tiles.map(tile => (
            <TileGroup tile={tile}/>
        ))}
    </Group>
);

RoomGroup.propTypes = {
    room: Shapes.Room,
};

export default RoomGroup;
