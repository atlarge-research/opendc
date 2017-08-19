import {combineReducers} from "redux";
import {ADD_TO_AUTHORIZATION_STORE, ADD_TO_SIMULATION_STORE, ADD_TO_USER_STORE} from "../actions/objects";

export const objects = combineReducers({
    simulation,
    authorization,
    user,
});

function simulation(state = {}, action) {
    switch (action.type) {
        case ADD_TO_SIMULATION_STORE:
            return Object.assign(
                state,
                {[action.simulation.id]: action.simulation}
            );
        default:
            return state;
    }
}

function authorization(state = {}, action) {
    switch (action.type) {
        case ADD_TO_AUTHORIZATION_STORE:
            return Object.assign(
                state,
                {[[action.authorization.userId, action.authorization.simulationId]]: action.authorization}
            );
        default:
            return state;
    }
}

function user(state = {}, action) {
    switch (action.type) {
        case ADD_TO_USER_STORE:
            return Object.assign(
                state,
                {[action.user.id]: action.user}
            );
        default:
            return state;
    }
}
