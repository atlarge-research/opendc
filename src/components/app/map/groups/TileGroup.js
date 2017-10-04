import PropTypes from "prop-types";
import React from "react";
import { Group } from "react-konva";
import RackContainer from "../../../../containers/app/map/RackContainer";
import Shapes from "../../../../shapes/index";
import {
  ROOM_DEFAULT_COLOR,
  ROOM_IN_CONSTRUCTION_COLOR
} from "../../../../util/colors";
import { convertLoadToSimulationColor } from "../../../../util/simulation-load";
import RoomTile from "../elements/RoomTile";

const TileGroup = ({ tile, newTile, inSimulation, roomLoad, onClick }) => {
  let tileObject;
  switch (tile.objectType) {
    case "RACK":
      tileObject = <RackContainer tile={tile} />;
      break;
    default:
      tileObject = null;
  }

  let color = ROOM_DEFAULT_COLOR;
  if (newTile) {
    color = ROOM_IN_CONSTRUCTION_COLOR;
  } else if (inSimulation && roomLoad >= 0) {
    color = convertLoadToSimulationColor(roomLoad);
  }

  return (
    <Group onClick={() => onClick(tile)}>
      <RoomTile tile={tile} color={color} />
      {tileObject}
    </Group>
  );
};

TileGroup.propTypes = {
  tile: Shapes.Tile,
  newTile: PropTypes.bool
};

export default TileGroup;
