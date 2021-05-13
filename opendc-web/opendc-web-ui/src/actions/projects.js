export const ADD_PROJECT = 'ADD_PROJECT'
export const ADD_PROJECT_SUCCEEDED = 'ADD_PROJECT_SUCCEEDED'
export const DELETE_PROJECT = 'DELETE_PROJECT'
export const DELETE_PROJECT_SUCCEEDED = 'DELETE_PROJECT_SUCCEEDED'
export const OPEN_PROJECT_SUCCEEDED = 'OPEN_PROJECT_SUCCEEDED'

export function addProject(name) {
    return (dispatch, getState) => {
        const { auth } = getState()
        dispatch({
            type: ADD_PROJECT,
            name,
            userId: auth.userId,
        })
    }
}

export function addProjectSucceeded(authorization) {
    return {
        type: ADD_PROJECT_SUCCEEDED,
        authorization,
    }
}

export function deleteProject(id) {
    return {
        type: DELETE_PROJECT,
        id,
    }
}

export function deleteProjectSucceeded(id) {
    return {
        type: DELETE_PROJECT_SUCCEEDED,
        id,
    }
}

export function openProjectSucceeded(id) {
    return {
        type: OPEN_PROJECT_SUCCEEDED,
        id,
    }
}
