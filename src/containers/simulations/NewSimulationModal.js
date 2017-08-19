import React from "react";
import {connect} from "react-redux";
import {addSimulation, closeNewSimulationModal} from "../../actions/simulations";
import TextInputModal from "../../components/modals/TextInputModal";

const NewSimulationModalComponent = ({visible, callback}) => (
    <TextInputModal title="New Simulation" label="Simulation title"
                    show={visible}
                    callback={callback}/>
);

const mapStateToProps = state => {
    return {
        visible: state.modals.newSimulationModalVisible
    };
};

const mapDispatchToProps = dispatch => {
    return {
        callback: (text) => {
            if (text) {
                dispatch(addSimulation(text));
            }
            dispatch(closeNewSimulationModal());
        }
    };
};

const NewSimulationModal = connect(
    mapStateToProps,
    mapDispatchToProps
)(NewSimulationModalComponent);

export default NewSimulationModal;
