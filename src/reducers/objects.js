import {combineReducers} from "redux";
import {ADD_TO_AUTHORIZATION_STORE, ADD_TO_SIMULATION_STORE, ADD_TO_USER_STORE} from "../actions/object-stores";

export const objects = combineReducers({
    simulations,
    authorizations,
    users,
});

function simulations(state = {}, action) {
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

function authorizations(state = {}, action) {
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

function users(state = {}, action) {
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
