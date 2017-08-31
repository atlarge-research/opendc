import {
    CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED,
    FETCH_LATEST_DATACENTER_SUCCEEDED,
    FINISH_NEW_ROOM_CONSTRUCTION,
    START_NEW_ROOM_CONSTRUCTION_SUCCEEDED
} from "../actions/topology";

export function currentDatacenterId(state = -1, action) {
    switch (action.type) {
        case FETCH_LATEST_DATACENTER_SUCCEEDED:
            return action.datacenterId;
        default:
            return state;
    }
}

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
