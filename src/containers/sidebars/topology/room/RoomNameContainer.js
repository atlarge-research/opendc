import {connect} from "react-redux";
import RoomNameComponent from "../../../../components/sidebars/topology/room/RoomNameComponent";

const mapStateToProps = state => {
    return {
        roomName: state.objects.room[state.interactionLevel.roomId].name,
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onEdit: () => dispatch(null), // FIXME
    };
};

const RoomNameContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(RoomNameComponent);

export default RoomNameContainer;
