export const ADD_TO_STORE = "ADD_TO_STORE";
export const ADD_PROP_TO_STORE_OBJECT = "ADD_PROP_TO_STORE_OBJECT";

export function addToStore(objectType, object) {
    return {
        type: ADD_TO_STORE,
        objectType,
        object
    };
}

export function addPropToStoreObject(objectType, objectId, propObject) {
    return {
        type: ADD_PROP_TO_STORE_OBJECT,
        objectType,
        objectId,
        propObject
    };
}
