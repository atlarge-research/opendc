import {connect} from "react-redux";
import {deleteSimulation} from "../../actions/simulations";
import SimulationActionButtons from "../../components/simulations/SimulationActionButtons";

const mapStateToProps = (state, ownProps) => {
    return {
        simulationId: ownProps.simulationId
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onViewUsers: (id) => {}, // TODO implement user viewing
        onDelete: (id) => dispatch(deleteSimulation(id)),
    };
};

const SimulationActions = connect(
    mapStateToProps,
    mapDispatchToProps
)(SimulationActionButtons);

export default SimulationActions;
