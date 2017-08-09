import {
    ADD_PROJECT,
    CLOSE_NEW_PROJECT_MODAL,
    OPEN_NEW_PROJECT_MODAL,
    SET_AUTH_VISIBILITY_FILTER
} from "../actions/projects";

export const authorizations = (state = [], action) => {
    switch (action.type) {
        case ADD_PROJECT:
            return [
                ...state,
                {
                    userId: -1,
                    simulation: {name: action.name, datetimeLastEdited: "2017-08-06T12:43:00"},
                    authorizationLevel: "OWN"
                }
            ];
        default:
            return state;
    }
};

export const newProjectModalVisible = (state = false, action) => {
    switch (action.type) {
        case OPEN_NEW_PROJECT_MODAL:
            return true;
        case CLOSE_NEW_PROJECT_MODAL:
            return false;
        default:
            return state;
    }
};

export const authVisibilityFilter = (state = "SHOW_ALL", action) => {
    switch (action.type) {
        case SET_AUTH_VISIBILITY_FILTER:
            return action.filter;
        default:
            return state;
    }
};
