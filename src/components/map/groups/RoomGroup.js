import React from "react";
import {Group} from "react-konva";
import Shapes from "../../../shapes/index";
import {deriveWallLocations} from "../../../util/tile-calculations";
import WallSegment from "../elements/WallSegment";
import TileGroup from "./TileGroup";

const RoomGroup = ({room}) => (
    <Group>
        {room.tiles.map(tile => (
            <TileGroup key={tile.id} tile={tile}/>
        ))}
        {deriveWallLocations(room).map((wallSegment, index) => (
            <WallSegment key={index} wallSegment={wallSegment}/>
        ))}
    </Group>
);

RoomGroup.propTypes = {
    room: Shapes.Room,
};

export default RoomGroup;
