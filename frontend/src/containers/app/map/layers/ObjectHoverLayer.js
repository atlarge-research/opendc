import { connect } from "react-redux";
import { addRackToTile } from "../../../../actions/topology/room";
import ObjectHoverLayerComponent from "../../../../components/app/map/layers/ObjectHoverLayerComponent";
import { findTileWithPosition } from "../../../../util/tile-calculations";

const mapStateToProps = state => {
  return {
    mapPosition: state.map.position,
    mapScale: state.map.scale,
    isEnabled: () => state.construction.inRackConstructionMode,
    isValid: (x, y) => {
      if (state.interactionLevel.mode !== "ROOM") {
        return false;
      }

      const currentRoom = state.objects.room[state.interactionLevel.roomId];
      const tiles = currentRoom.tileIds.map(
        tileId => state.objects.tile[tileId]
      );
      const tile = findTileWithPosition(tiles, x, y);

      return !(tile === null || tile.objectType);
    }
  };
};

const mapDispatchToProps = dispatch => {
  return {
    onClick: (x, y) => dispatch(addRackToTile(x, y))
  };
};

const ObjectHoverLayer = connect(mapStateToProps, mapDispatchToProps)(
  ObjectHoverLayerComponent
);

export default ObjectHoverLayer;
