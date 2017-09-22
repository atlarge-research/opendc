import {connect} from "react-redux";
import {goToTick} from "../../../actions/simulation/tick";
import TimelineControlsComponent from "../../../components/app/timeline/TimelineControlsComponent";

const mapStateToProps = state => {
    let sectionTicks = [];
    if (state.currentExperimentId !== -1) {
        const sectionIds = state.objects.path[state.objects.experiment[state.currentExperimentId].pathId].sectionIds;
        if (sectionIds) {
            sectionTicks = sectionIds
                .filter(sectionId => state.objects.section[sectionId].startTick !== 0)
                .map(sectionId => state.objects.section[sectionId].startTick);
        }
    }

    return {
        currentTick: state.currentTick,
        lastSimulatedTick: state.lastSimulatedTick,
        sectionTicks,
    };
};

const mapDispatchToProps = dispatch => {
    return {
        goToTick: (tick) => dispatch(goToTick(tick)),
    };
};

const TimelineControlsContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(TimelineControlsComponent);

export default TimelineControlsContainer;
