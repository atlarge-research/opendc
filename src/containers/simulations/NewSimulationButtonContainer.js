import {connect} from "react-redux";
import {openNewSimulationModal} from "../../actions/modals/simulations";
import NewSimulationButtonComponent from "../../components/simulations/NewSimulationButtonComponent";

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(openNewSimulationModal())
    };
};

const NewSimulationButtonContainer = connect(
    undefined,
    mapDispatchToProps
)(NewSimulationButtonComponent);

export default NewSimulationButtonContainer;
