import {connect} from "react-redux";
import {deleteExperiment} from "../../actions/experiments";
import ExperimentRowComponent from "../../components/experiments/ExperimentRowComponent";

const mapStateToProps = (state, ownProps) => {
    const experiment = Object.assign({}, state.objects.experiment[ownProps.experimentId]);
    experiment.trace = state.objects.trace[experiment.traceId];
    experiment.scheduler = state.objects.scheduler[experiment.schedulerName];
    experiment.path = state.objects.path[experiment.pathId];

    return {
        experiment,
        simulationId: state.currentSimulationId,
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onDelete: id => dispatch(deleteExperiment(id))
    };
};

const ExperimentRowContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(ExperimentRowComponent);

export default ExperimentRowContainer;
