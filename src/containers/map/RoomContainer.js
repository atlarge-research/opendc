import {connect} from "react-redux";
import {goFromBuildingToRoom} from "../../actions/interaction-level";
import RoomGroup from "../../components/map/groups/RoomGroup";

const mapStateToProps = (state, ownProps) => {
    return {
        interactionLevel: state.interactionLevel,
        currentRoomInConstruction: state.construction.currentRoomInConstruction,
        room: state.objects.room[ownProps.roomId],
    };
};

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onClick: () => dispatch(goFromBuildingToRoom(ownProps.roomId)),
    };
};

const RoomContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(RoomGroup);

export default RoomContainer;
