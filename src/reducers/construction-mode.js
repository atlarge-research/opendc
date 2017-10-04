import { combineReducers } from "redux";
import { OPEN_EXPERIMENT_SUCCEEDED } from "../actions/experiments";
import {
  CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED,
  FINISH_NEW_ROOM_CONSTRUCTION,
  START_NEW_ROOM_CONSTRUCTION_SUCCEEDED
} from "../actions/topology/building";
import {
  START_RACK_CONSTRUCTION,
  STOP_RACK_CONSTRUCTION
} from "../actions/topology/room";

export function currentRoomInConstruction(state = -1, action) {
  switch (action.type) {
    case START_NEW_ROOM_CONSTRUCTION_SUCCEEDED:
      return action.roomId;
    case CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED:
    case FINISH_NEW_ROOM_CONSTRUCTION:
    case OPEN_EXPERIMENT_SUCCEEDED:
      return -1;
    default:
      return state;
  }
}

export function inRackConstructionMode(state = false, action) {
  switch (action.type) {
    case START_RACK_CONSTRUCTION:
      return true;
    case STOP_RACK_CONSTRUCTION:
    case OPEN_EXPERIMENT_SUCCEEDED:
      return false;
    default:
      return state;
  }
}

export const construction = combineReducers({
  currentRoomInConstruction,
  inRackConstructionMode
});
