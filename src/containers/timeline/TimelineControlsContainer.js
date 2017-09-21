import {connect} from "react-redux";
import TimelineControlsComponent from "../../components/timeline/TimelineControlsComponent";

const mapStateToProps = state => {
    let sectionTicks = [];
    if (state.currentExperimentId !== -1) {
        const sectionIds = state.objects.path[state.objects.experiment[state.currentExperimentId].pathId].sectionIds;
        if (sectionIds) {
            sectionTicks = sectionIds.map(sectionId => state.objects.section[sectionId].startTick);
        }
    }

    return {
        currentTick: state.currentTick,
        lastSimulatedTick: state.lastSimulatedTick,
        sectionTicks,
    };
};

const TimelineControlsContainer = connect(
    mapStateToProps
)(TimelineControlsComponent);

export default TimelineControlsContainer;
