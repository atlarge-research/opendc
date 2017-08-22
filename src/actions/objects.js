export const ADD_TO_STORE = "ADD_TO_STORE";

export function addToStore(objectType, object) {
    return {
        type: ADD_TO_STORE,
        objectType,
        object
    };
}
