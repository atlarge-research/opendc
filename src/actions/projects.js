export const SET_AUTH_VISIBILITY_FILTER = "SET_AUTH_VISIBILITY_FILTER";
export const OPEN_NEW_PROJECT_MODAL = "OPEN_NEW_PROJECT_MODAL";
export const CLOSE_NEW_PROJECT_MODAL = "CLOSE_PROJECT_POPUP";
export const ADD_PROJECT = "ADD_PROJECT";
export const DELETE_PROJECT = "DELETE_PROJECT";
export const OPEN_PROJECT = "OPEN_PROJECT";

export function setAuthVisibilityFilter(filter) {
    return {
        type: SET_AUTH_VISIBILITY_FILTER,
        filter: filter
    };
}

export function openNewProjectModal() {
    return {
        type: OPEN_NEW_PROJECT_MODAL
    };
}

export function closeNewProjectModal() {
    return {
        type: CLOSE_NEW_PROJECT_MODAL
    };
}

export function addProject(name) {
    return {
        type: ADD_PROJECT,
        name
    };
}

export function deleteProject(id) {
    return {
        type: DELETE_PROJECT,
        id
    };
}

export function openProject(id) {
    return {
        type: OPEN_PROJECT,
        id
    };
}
