import {GO_FROM_BUILDING_TO_ROOM, GO_FROM_ROOM_TO_BUILDING} from "../actions/interaction-level";

export function interactionLevel(state = {mode: "BUILDING"}, action) {
    switch (action.type) {
        case GO_FROM_BUILDING_TO_ROOM:
            return {
                mode: "ROOM",
                roomId: action.roomId
            };
        case GO_FROM_ROOM_TO_BUILDING:
            return {
                mode: "BUILDING"
            };
        default:
            return state;
    }
}
