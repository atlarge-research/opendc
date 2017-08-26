import {connect} from "react-redux";
import {goFromRoomToObject} from "../../actions/interaction-level";
import TileGroup from "../../components/map/groups/TileGroup";

const mapStateToProps = state => {
    return {
        interactionLevel: state.interactionLevel
    };
};

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onClick: () => {
            if (ownProps.tile.objectType) {
                dispatch(goFromRoomToObject(ownProps.tile.id))
            }
        }
    };
};

const TileContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(TileGroup);

export default TileContainer;
