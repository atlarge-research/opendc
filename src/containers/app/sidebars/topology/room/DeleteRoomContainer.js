import { connect } from "react-redux";
import { openDeleteRoomModal } from "../../../../../actions/modals/topology";
import DeleteRoomComponent from "../../../../../components/app/sidebars/topology/room/DeleteRoomComponent";

const mapDispatchToProps = dispatch => {
  return {
    onClick: () => dispatch(openDeleteRoomModal())
  };
};

const DeleteRoomContainer = connect(undefined, mapDispatchToProps)(
  DeleteRoomComponent
);

export default DeleteRoomContainer;
