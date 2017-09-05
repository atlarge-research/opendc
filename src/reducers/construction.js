import {combineReducers} from "redux";
import {
    CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED,
    FINISH_NEW_ROOM_CONSTRUCTION,
    START_NEW_ROOM_CONSTRUCTION_SUCCEEDED,
    START_OBJECT_CONSTRUCTION,
    STOP_OBJECT_CONSTRUCTION
} from "../actions/topology";

export function currentRoomInConstruction(state = -1, action) {
    switch (action.type) {
        case START_NEW_ROOM_CONSTRUCTION_SUCCEEDED:
            return action.roomId;
        case CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED:
        case FINISH_NEW_ROOM_CONSTRUCTION:
            return -1;
        default:
            return state;
    }
}

export function inObjectConstructionMode(state = false, action) {
    switch (action.type) {
        case START_OBJECT_CONSTRUCTION:
            return true;
        case STOP_OBJECT_CONSTRUCTION:
            return false;
        default:
            return state;
    }
}

export const construction = combineReducers({
    currentRoomInConstruction,
    inObjectConstructionMode,
});
