import {connect} from "react-redux";
import {goFromRoomToRack} from "../../actions/interaction-level";
import TileGroup from "../../components/map/groups/TileGroup";

const mapStateToProps = (state, ownProps) => {
    return {
        interactionLevel: state.interactionLevel,
        tile: state.objects.tile[ownProps.tileId],
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onClick: tile => {
            if (tile.objectType) {
                dispatch(goFromRoomToRack(tile.id))
            }
        }
    };
};

const TileContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(TileGroup);

export default TileContainer;
