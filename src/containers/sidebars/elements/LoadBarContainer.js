import {connect} from "react-redux";
import LoadBarComponent from "../../../components/sidebars/elements/LoadBarComponent";
import {getStateLoad} from "../../../util/simulation-load";

const mapStateToProps = (state, ownProps) => {
    let percent = 0;
    let enabled = false;

    const objectStates = state.states[ownProps.objectType];
    if (objectStates[state.currentTick] && objectStates[state.currentTick][ownProps.objectId]) {
        percent = Math.floor(100 * getStateLoad(state.loadMetric, objectStates[state.currentTick][ownProps.objectId]));
        enabled = true;
    }

    return {
        percent,
        enabled
    };
};

const LoadBarContainer = connect(
    mapStateToProps
)(LoadBarComponent);

export default LoadBarContainer;
