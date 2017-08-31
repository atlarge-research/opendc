import {connect} from "react-redux";
import {toggleTileAtLocation} from "../../../actions/topology";
import HoverTileLayerComponent from "../../../components/map/layers/HoverTileLayerComponent";
import {
    deriveValidNextTilePositions,
    findPositionInPositions,
    findPositionInRooms
} from "../../../util/tile-calculations";

const mapStateToProps = state => {
    return {
        currentRoomInConstruction: state.currentRoomInConstruction,
        isValid: (x, y) => {
            const newRoom = Object.assign({}, state.objects.room[state.currentRoomInConstruction]);
            const oldRooms = Object.keys(state.objects.room)
                .map(id => Object.assign({}, state.objects.room[id]))
                .filter(room => room.id !== state.currentRoomInConstruction);

            [...oldRooms, newRoom].forEach(room => {
                room.tiles = room.tileIds.map(tileId => state.objects.tile[tileId]);
            });
            if (newRoom.tileIds.length === 0) {
                return findPositionInRooms(oldRooms, x, y) === -1;
            }

            const validNextPositions = deriveValidNextTilePositions(oldRooms, newRoom.tiles);
            return findPositionInPositions(validNextPositions, x, y) !== -1;
        },
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onClick: (x, y) => dispatch(toggleTileAtLocation(x, y)),
    };
};

const HoverTileLayer = connect(
    mapStateToProps,
    mapDispatchToProps
)(HoverTileLayerComponent);

export default HoverTileLayer;
