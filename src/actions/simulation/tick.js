import { getDatacenterIdOfTick } from "../../util/timeline";
import { setCurrentDatacenter } from "../topology/building";

export const GO_TO_TICK = "GO_TO_TICK";
export const SET_LAST_SIMULATED_TICK = "SET_LAST_SIMULATED_TICK";

export function incrementTick() {
  return (dispatch, getState) => {
    const { currentTick } = getState();
    dispatch(goToTick(currentTick + 1));
  };
}

export function goToTick(tick) {
  return (dispatch, getState) => {
    const state = getState();

    let sections = [];
    if (state.currentExperimentId !== -1) {
      const sectionIds =
        state.objects.path[
          state.objects.experiment[state.currentExperimentId].pathId
        ].sectionIds;

      if (sectionIds) {
        sections = sectionIds.map(
          sectionId => state.objects.section[sectionId]
        );
      }
    }

    const newDatacenterId = getDatacenterIdOfTick(tick, sections);
    if (state.currentDatacenterId !== newDatacenterId) {
      dispatch(setCurrentDatacenter(newDatacenterId));
    }

    dispatch({
      type: GO_TO_TICK,
      tick
    });
  };
}

export function setLastSimulatedTick(tick) {
  return {
    type: SET_LAST_SIMULATED_TICK,
    tick
  };
}
