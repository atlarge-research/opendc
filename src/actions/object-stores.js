export const ADD_TO_SIMULATION_STORE = "ADD_TO_SIMULATION_STORE";
export const ADD_TO_AUTHORIZATION_STORE = "ADD_TO_AUTHORIZATION_STORE";
export const ADD_TO_USER_STORE = "ADD_TO_USER_STORE";

export function addToSimulationStore(simulation) {
    return {
        type: ADD_TO_SIMULATION_STORE,
        simulation
    };
}

export function addToAuthorizationStore(authorization) {
    return {
        type: ADD_TO_AUTHORIZATION_STORE,
        authorization
    };
}

export function addToUserStore(user) {
    return {
        type: ADD_TO_USER_STORE,
        user
    }
}
