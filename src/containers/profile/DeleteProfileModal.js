import React from "react";
import {connect} from "react-redux";
import {closeDeleteProfileModal} from "../../actions/profile";
import {deleteCurrentUser} from "../../actions/users";
import ConfirmationModal from "../../components/modals/ConfirmationModal";

const NewSimulationModalComponent = ({visible, callback}) => (
    <ConfirmationModal title="Delete my account"
                       message="Are you sure you want do delete your OpenDC account?"
                       show={visible}
                       callback={callback}/>
);

const mapStateToProps = state => {
    return {
        visible: state.modals.deleteProfileModalVisible
    };
};

const mapDispatchToProps = dispatch => {
    return {
        callback: (isConfirmed) => {
            if (isConfirmed) {
                dispatch(deleteCurrentUser());
            }
            dispatch(closeDeleteProfileModal());
        }
    };
};

const NewSimulationModal = connect(
    mapStateToProps,
    mapDispatchToProps
)(NewSimulationModalComponent);

export default NewSimulationModal;
