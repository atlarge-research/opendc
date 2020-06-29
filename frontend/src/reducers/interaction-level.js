import { OPEN_EXPERIMENT_SUCCEEDED } from "../actions/experiments";
import {
  GO_DOWN_ONE_INTERACTION_LEVEL,
  GO_FROM_BUILDING_TO_ROOM,
  GO_FROM_RACK_TO_MACHINE,
  GO_FROM_ROOM_TO_RACK
} from "../actions/interaction-level";
import { OPEN_SIMULATION_SUCCEEDED } from "../actions/simulations";
import { SET_CURRENT_DATACENTER } from "../actions/topology/building";

export function interactionLevel(state = { mode: "BUILDING" }, action) {
  switch (action.type) {
    case OPEN_EXPERIMENT_SUCCEEDED:
    case OPEN_SIMULATION_SUCCEEDED:
    case SET_CURRENT_DATACENTER:
      return {
        mode: "BUILDING"
      };
    case GO_FROM_BUILDING_TO_ROOM:
      return {
        mode: "ROOM",
        roomId: action.roomId
      };
    case GO_FROM_ROOM_TO_RACK:
      return {
        mode: "RACK",
        roomId: state.roomId,
        tileId: action.tileId
      };
    case GO_FROM_RACK_TO_MACHINE:
      return {
        mode: "MACHINE",
        roomId: state.roomId,
        tileId: state.tileId,
        position: action.position
      };
    case GO_DOWN_ONE_INTERACTION_LEVEL:
      if (state.mode === "ROOM") {
        return {
          mode: "BUILDING"
        };
      } else if (state.mode === "RACK") {
        return {
          mode: "ROOM",
          roomId: state.roomId
        };
      } else if (state.mode === "MACHINE") {
        return {
          mode: "RACK",
          roomId: state.roomId,
          tileId: state.tileId
        };
      } else {
        return state;
      }
    default:
      return state;
  }
}
