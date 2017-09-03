import {connect} from "react-redux";
import RoomTypeComponent from "../../../../components/sidebars/topology/room/RoomTypeComponent";

const mapStateToProps = state => {
    return {
        roomType: state.objects.room[state.interactionLevel.roomId].roomType,
    };
};

const RoomNameContainer = connect(
    mapStateToProps
)(RoomTypeComponent);

export default RoomNameContainer;
