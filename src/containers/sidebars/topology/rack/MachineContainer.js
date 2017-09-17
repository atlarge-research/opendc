import {connect} from "react-redux";
import {goFromRackToMachine} from "../../../../actions/interaction-level";
import MachineComponent from "../../../../components/sidebars/topology/rack/MachineComponent";
import {getStateLoad} from "../../../../util/simulation-load";

const mapStateToProps = (state, ownProps) => {
    const machine = state.objects.machine[ownProps.machineId];
    const inSimulation = state.currentExperimentId !== -1;

    let machineLoad = undefined;
    if (inSimulation) {
        if (state.states.machine[state.currentTick] && state.states.machine[state.currentTick][machine.id]) {
            machineLoad = getStateLoad(state.loadMetric, state.states.machine[state.currentTick][machine.id]);
        }
    }

    return {
        machine,
        inSimulation,
        machineLoad
    };
};

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onClick: () => dispatch(goFromRackToMachine(ownProps.position)),
    };
};

const MachineContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(MachineComponent);

export default MachineContainer;
