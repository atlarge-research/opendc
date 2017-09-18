import {connect} from "react-redux";
import TimelineLabelsComponent from "../../components/timeline/TimelineLabelsComponent";

const mapStateToProps = state => {
    return {
        currentTick: state.currentTick,
        lastSimulatedTick: state.lastSimulatedTick,
    };
};

const TimelineLabelsContainer = connect(
    mapStateToProps
)(TimelineLabelsComponent);

export default TimelineLabelsContainer;
