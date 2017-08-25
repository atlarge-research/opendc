import {connect} from "react-redux";
import {goFromBuildingToRoom} from "../../actions/interaction-level";
import RoomGroup from "../../components/map/groups/RoomGroup";

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onClick: () => dispatch(goFromBuildingToRoom(ownProps.room.id))
    };
};

const RoomContainer = connect(
    undefined,
    mapDispatchToProps
)(RoomGroup);

export default RoomContainer;
