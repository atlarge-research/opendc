import {connect} from "react-redux";
import {goFromBuildingToRoom} from "../../actions/interaction-level";
import RoomGroup from "../../components/map/groups/RoomGroup";

const mapStateToProps = state => {
    return {
        interactionLevel: state.interactionLevel
    };
};

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onClick: () => dispatch(goFromBuildingToRoom(ownProps.room.id))
    };
};

const RoomContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(RoomGroup);

export default RoomContainer;
