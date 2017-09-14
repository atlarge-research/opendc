import {connect} from "react-redux";
import ExperimentListComponent from "../../components/experiments/ExperimentListComponent";

const mapStateToProps = state => {
    if (!state.currentSimulationId) {
        return {
            experimentIds: [],
        };
    }

    const experimentIds = state.objects.simulation[state.currentSimulationId].experimentIds;
    if (experimentIds) {
        return {
            experimentIds,
        };
    }

    return {
        experimentIds: [],
    };
};

const ExperimentListContainer = connect(
    mapStateToProps
)(ExperimentListComponent);

export default ExperimentListContainer;
