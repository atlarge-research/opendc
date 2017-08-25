import React from "react";
import {Group} from "react-konva";
import Shapes from "../../../shapes/index";
import TileGroup from "./TileGroup";

const RoomGroup = ({room}) => (
    <Group>
        {room.tiles.map(tile => (
            <TileGroup key={tile.id} tile={tile}/>
        ))}
    </Group>
);

RoomGroup.propTypes = {
    room: Shapes.Room,
};

export default RoomGroup;
