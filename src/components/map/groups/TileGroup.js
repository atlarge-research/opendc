import PropTypes from "prop-types";
import React from "react";
import {Group} from "react-konva";
import Shapes from "../../../shapes/index";
import RoomTile from "../elements/RoomTile";
import RackGroup from "./RackGroup";

const TileGroup = ({tile, newTile, onClick}) => {
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
            onClick={() => onClick(tile)}
        >
            <RoomTile tile={tile} newTile={newTile}/>
            {tileObject}
        </Group>
    );
};

TileGroup.propTypes = {
    tile: Shapes.Tile,
    newTile: PropTypes.bool,
};

export default TileGroup;
