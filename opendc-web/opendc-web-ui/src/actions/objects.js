export const ADD_TO_STORE = 'ADD_TO_STORE'
export const ADD_PROP_TO_STORE_OBJECT = 'ADD_PROP_TO_STORE_OBJECT'
export const ADD_ID_TO_STORE_OBJECT_LIST_PROP = 'ADD_ID_TO_STORE_OBJECT_LIST_PROP'
export const REMOVE_ID_FROM_STORE_OBJECT_LIST_PROP = 'REMOVE_ID_FROM_STORE_OBJECT_LIST_PROP'

export function addToStore(objectType, object) {
    return {
        type: ADD_TO_STORE,
        objectType,
        object,
    }
}

export function addPropToStoreObject(objectType, objectId, propObject) {
    return {
        type: ADD_PROP_TO_STORE_OBJECT,
        objectType,
        objectId,
        propObject,
    }
}

export function addIdToStoreObjectListProp(objectType, objectId, propName, id) {
    return {
        type: ADD_ID_TO_STORE_OBJECT_LIST_PROP,
        objectType,
        objectId,
        propName,
        id,
    }
}

export function removeIdFromStoreObjectListProp(objectType, objectId, propName, id) {
    return {
        type: REMOVE_ID_FROM_STORE_OBJECT_LIST_PROP,
        objectType,
        objectId,
        propName,
        id,
    }
}
