import React from "react";
import {Group} from "react-konva";
import Shapes from "../../../shapes/index";
import RoomTile from "../elements/RoomTile";
import RackGroup from "./RackGroup";

const TileGroup = ({tile, onClick}) => {
    let tileObject;
    switch (tile.objectType) {
        case "RACK":
            tileObject = <RackGroup tile={tile}/>;
            break;
        default:
            tileObject = null;
    }

    return (
        <Group
            onClick={onClick}
        >
            <RoomTile tile={tile}/>
            {tileObject}
        </Group>
    );
};

TileGroup.propTypes = {
    tile: Shapes.Tile,
};

export default TileGroup;
