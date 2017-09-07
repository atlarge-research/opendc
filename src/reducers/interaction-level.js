import {
    GO_DOWN_ONE_INTERACTION_LEVEL,
    GO_FROM_BUILDING_TO_ROOM,
    GO_FROM_ROOM_TO_RACK
} from "../actions/interaction-level";

export function interactionLevel(state = {mode: "BUILDING"}, action) {
    switch (action.type) {
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
            } else {
                return state;
            }
        default:
            return state;
    }
}
