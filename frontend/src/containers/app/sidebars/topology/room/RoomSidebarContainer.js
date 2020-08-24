import { connect } from "react-redux";
import RoomSidebarComponent from "../../../../../components/app/sidebars/topology/room/RoomSidebarComponent";

const mapStateToProps = state => {
  return {
    roomId: state.interactionLevel.roomId,
    roomType: state.objects.room[state.interactionLevel.roomId].roomType,
    inSimulation: state.currentExperimentId !== -1
  };
};

const RoomSidebarContainer = connect(mapStateToProps)(RoomSidebarComponent);

export default RoomSidebarContainer;
