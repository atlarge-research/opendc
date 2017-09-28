export const ADD_BATCH_TO_STATES = "ADD_BATCH_TO_STATES";

export function addBatchToStates(objectType, objects) {
    return {
        type: ADD_BATCH_TO_STATES,
        objectType,
        objects
    };
}
