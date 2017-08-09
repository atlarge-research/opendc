import React from "react";
import {connect} from "react-redux";
import {addProject, closeNewProjectModal} from "../../actions/projects";
import TextInputModal from "../../components/modals/TextInputModal";

const NewProjectModalComponent = ({visible, callback}) => (
    <TextInputModal title="New Project" label="Project title"
                    show={visible}
                    callback={callback}/>
);

const mapStateToProps = state => {
    return {
        visible: state.newProjectModalVisible
    };
};

const mapDispatchToProps = dispatch => {
    return {
        callback: (text) => {
            if (text) {
                dispatch(addProject(text));
            }
            dispatch(closeNewProjectModal());
        }
    };
};

const NewProjectModal = connect(
    mapStateToProps,
    mapDispatchToProps
)(NewProjectModalComponent);

export default NewProjectModal;
