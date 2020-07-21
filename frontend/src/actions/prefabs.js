export const ADD_PREFAB = 'ADD_PREFAB'
export const ADD_PREFAB_SUCCEEDED = 'ADD_PREFAB_SUCCEEDED'
export const DELETE_PREFAB = 'DELETE_PREFAB'
export const DELETE_PREFAB_SUCCEEDED = 'DELETE_PREFAB_SUCCEEDED'
export const OPEN_PREFAB_SUCCEEDED = 'OPEN_PREFAB_SUCCEEDED'


export function addPrefab(name) {
    return (dispatch, getState) => {
        const { auth } = getState()
        dispatch({
            type: ADD_PREFAB,
            name,
            userId: auth.userId,
        })
    }
}

export function addPrefabSucceeded(authorization) {
    return {
        type: ADD_PREFAB_SUCCEEDED,
        authorization,
    }
}

export function deletePrefab(id) {
    return {
        type: DELETE_PREFAB,
        id,
    }
}

export function deletePrefabSucceeded(id) {
    return {
        type: DELETE_PREFAB_SUCCEEDED,
        id,
    }
}

export function openPrefabSucceeded(id) {
    return {
        type: OPEN_PREFAB_SUCCEEDED,
        id,
    }
}
