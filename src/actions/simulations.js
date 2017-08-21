export const SET_AUTH_VISIBILITY_FILTER = "SET_AUTH_VISIBILITY_FILTER";
export const OPEN_NEW_SIMULATION_MODAL = "OPEN_NEW_SIMULATION_MODAL";
export const CLOSE_NEW_SIMULATION_MODAL = "CLOSE_SIMULATION_POPUP";
export const ADD_SIMULATION = "ADD_SIMULATION";
export const ADD_SIMULATION_SUCCEEDED = "ADD_SIMULATION_SUCCEEDED";
export const DELETE_SIMULATION = "DELETE_SIMULATION";
export const DELETE_SIMULATION_SUCCEEDED = "DELETE_SIMULATION_SUCCEEDED";
export const OPEN_SIMULATION_SUCCEEDED = "OPEN_SIMULATION_SUCCEEDED";

export function setAuthVisibilityFilter(filter) {
    return {
        type: SET_AUTH_VISIBILITY_FILTER,
        filter: filter
    };
}

export function openNewSimulationModal() {
    return {
        type: OPEN_NEW_SIMULATION_MODAL
    };
}

export function closeNewSimulationModal() {
    return {
        type: CLOSE_NEW_SIMULATION_MODAL
    };
}

export function addSimulation(name) {
    return (dispatch, getState) => {
        const {auth} = getState();
        dispatch({
            type: ADD_SIMULATION,
            name,
            userId: auth.userId
        });
    };
}

export function addSimulationSucceeded(authorization) {
    return {
        type: ADD_SIMULATION_SUCCEEDED,
        authorization
    };
}

export function deleteSimulation(id) {
    return {
        type: DELETE_SIMULATION,
        id
    };
}

export function deleteSimulationSucceeded(id) {
    return {
        type: DELETE_SIMULATION_SUCCEEDED,
        id
    };
}

export function openSimulationSucceeded(id) {
    return {
        type: OPEN_SIMULATION_SUCCEEDED,
        id
    };
}
