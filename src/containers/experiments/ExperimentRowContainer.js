import {connect} from "react-redux";
import ExperimentRowComponent from "../../components/experiments/ExperimentRowComponent";

const mapStateToProps = (state, ownProps) => {
    const experiment = Object.assign({}, state.objects.experiment[ownProps.experimentId]);
    experiment.trace = state.objects.trace[experiment.traceId];
    experiment.scheduler = state.objects.scheduler[experiment.schedulerName];
    experiment.path = state.objects.path[experiment.pathId];

    return {
        experiment,
    };
};

const ExperimentRowContainer = connect(
    mapStateToProps
)(ExperimentRowComponent);

export default ExperimentRowContainer;
