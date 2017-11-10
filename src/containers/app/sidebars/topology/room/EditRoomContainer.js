import { connect } from "react-redux";
import {
  finishRoomEdit,
  startRoomEdit
} from "../../../../../actions/topology/building";
import EditRoomComponent from "../../../../../components/app/sidebars/topology/room/EditRoomComponent";

const mapStateToProps = state => {
  return {
    isEditing: state.construction.currentRoomInConstruction !== -1,
    isInRackConstructionMode: state.construction.inRackConstructionMode
  };
};

const mapDispatchToProps = dispatch => {
  return {
    onEdit: () => dispatch(startRoomEdit()),
    onFinish: () => dispatch(finishRoomEdit())
  };
};

const EditRoomContainer = connect(mapStateToProps, mapDispatchToProps)(
  EditRoomComponent
);

export default EditRoomContainer;
