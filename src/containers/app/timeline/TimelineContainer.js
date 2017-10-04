import { connect } from "react-redux";
import { pauseSimulation } from "../../../actions/simulation/playback";
import { incrementTick } from "../../../actions/simulation/tick";
import { setCurrentDatacenter } from "../../../actions/topology/building";
import TimelineComponent from "../../../components/app/timeline/TimelineComponent";

const mapStateToProps = state => {
  let sections = [];
  if (state.currentExperimentId !== -1) {
    const sectionIds =
      state.objects.path[
        state.objects.experiment[state.currentExperimentId].pathId
      ].sectionIds;

    if (sectionIds) {
      sections = sectionIds.map(sectionId => state.objects.section[sectionId]);
    }
  }

  return {
    isPlaying: state.isPlaying,
    currentTick: state.currentTick,
    lastSimulatedTick: state.lastSimulatedTick,
    currentDatacenterId: state.currentDatacenterId,
    sections
  };
};

const mapDispatchToProps = dispatch => {
  return {
    incrementTick: () => dispatch(incrementTick()),
    pauseSimulation: () => dispatch(pauseSimulation()),
    setCurrentDatacenter: id => dispatch(setCurrentDatacenter(id))
  };
};

const TimelineContainer = connect(mapStateToProps, mapDispatchToProps)(
  TimelineComponent
);

export default TimelineContainer;
