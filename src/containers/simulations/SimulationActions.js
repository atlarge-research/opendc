import {connect} from "react-redux";
import {deleteSimulation, openSimulation} from "../../actions/simulations";
import SimulationActionButtons from "../../components/simulations/SimulationActionButtons";

const mapStateToProps = (state, ownProps) => {
    return {
        simulationId: ownProps.simulationId
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onOpen: (id) => dispatch(openSimulation(id)),
        onViewUsers: (id) => {},
        onDelete: (id) => dispatch(deleteSimulation(id)),
    };
};

const SimulationActions = connect(
    mapStateToProps,
    mapDispatchToProps
)(SimulationActionButtons);

export default SimulationActions;
