import {connect} from "react-redux";
import RoomSidebarComponent from "../../../../components/sidebars/topology/room/RoomSidebarComponent";

const mapStateToProps = state => {
    return {
        roomType: state.objects.room[state.interactionLevel.roomId].roomType,
    };
};

const RoomSidebarContainer = connect(
    mapStateToProps
)(RoomSidebarComponent);

export default RoomSidebarContainer;
