import {combineReducers} from "redux";
import {CLOSE_DELETE_PROFILE_MODAL, OPEN_DELETE_PROFILE_MODAL} from "../actions/profile";
import {CLOSE_NEW_SIMULATION_MODAL, OPEN_NEW_SIMULATION_MODAL} from "../actions/simulations";

function newSimulationModalVisible(state = false, action) {
    switch (action.type) {
        case OPEN_NEW_SIMULATION_MODAL:
            return true;
        case CLOSE_NEW_SIMULATION_MODAL:
            return false;
        default:
            return state;
    }
}

function deleteProfileModalVisible(state = false, action) {
    switch (action.type) {
        case OPEN_DELETE_PROFILE_MODAL:
            return true;
        case CLOSE_DELETE_PROFILE_MODAL:
            return false;
        default:
            return state;
    }
}

export const modals = combineReducers({
    newSimulationModalVisible,
    deleteProfileModalVisible,
});
