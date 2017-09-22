import {connect} from "react-redux";
import LoadChartComponent from "../../../../components/app/sidebars/elements/LoadChartComponent";
import {getStateLoad} from "../../../../util/simulation-load";

const mapStateToProps = (state, ownProps) => {
    const data = [];

    if (state.lastSimulatedTick !== -1) {
        const objectStates = state.states[ownProps.objectType];
        Object.keys(objectStates).forEach(tick => {
            if (objectStates[tick][ownProps.objectId]) {
                data.push({x: tick, y: getStateLoad(state.loadMetric, objectStates[tick][ownProps.objectId])});
            }
        });
    }

    return {
        data,
        currentTick: state.currentTick,
    };
};

const LoadChartContainer = connect(
    mapStateToProps
)(LoadChartComponent);

export default LoadChartContainer;
