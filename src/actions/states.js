export const ADD_TO_STATES = "ADD_TO_STATES";

export function addToStates(objectType, tick, object) {
    return {
        type: ADD_TO_STATES,
        objectType,
        tick,
        object
    };
}
