import {
    ADD_PROJECT,
    CLOSE_NEW_PROJECT_MODAL,
    DELETE_PROJECT,
    OPEN_NEW_PROJECT_MODAL,
    SET_AUTH_VISIBILITY_FILTER
} from "../actions/projects";
import {FETCH_AUTHORIZATIONS_OF_CURRENT_USER_SUCCEEDED} from "../actions/users";

export function authorizationsOfCurrentUser(state = [], action) {
    switch (action.type) {
        case FETCH_AUTHORIZATIONS_OF_CURRENT_USER_SUCCEEDED:
            return action.authorizationsOfCurrentUser;
        case ADD_PROJECT:
            return [
                ...state,
                {
                    userId: -1,
                    simulation: {name: action.name, datetimeLastEdited: "2017-08-06T12:43:00", id: state.length},
                    authorizationLevel: "OWN"
                }
            ];
        case DELETE_PROJECT:
            return [];
        default:
            return state;
    }
}

export function newProjectModalVisible(state = false, action) {
    switch (action.type) {
        case OPEN_NEW_PROJECT_MODAL:
            return true;
        case CLOSE_NEW_PROJECT_MODAL:
            return false;
        default:
            return state;
    }
}

export function authVisibilityFilter(state = "SHOW_ALL", action) {
    switch (action.type) {
        case SET_AUTH_VISIBILITY_FILTER:
            return action.filter;
        default:
            return state;
    }
}
