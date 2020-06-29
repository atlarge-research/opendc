import React from "react";
import { connect } from "react-redux";
import { closeDeleteMachineModal } from "../../actions/modals/topology";
import { deleteMachine } from "../../actions/topology/machine";
import ConfirmationModal from "../../components/modals/ConfirmationModal";

const DeleteMachineModalComponent = ({ visible, callback }) => (
  <ConfirmationModal
    title="Delete this machine"
    message="Are you sure you want to delete this machine?"
    show={visible}
    callback={callback}
  />
);

const mapStateToProps = state => {
  return {
    visible: state.modals.deleteMachineModalVisible
  };
};

const mapDispatchToProps = dispatch => {
  return {
    callback: isConfirmed => {
      if (isConfirmed) {
        dispatch(deleteMachine());
      }
      dispatch(closeDeleteMachineModal());
    }
  };
};

const DeleteMachineModal = connect(mapStateToProps, mapDispatchToProps)(
  DeleteMachineModalComponent
);

export default DeleteMachineModal;
