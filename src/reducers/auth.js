import {LOG_IN_SUCCEEDED, LOG_OUT} from "../actions/auth";

export function auth(state = {}, action) {
    switch (action.type) {
        case LOG_IN_SUCCEEDED:
            return action.payload;
        case LOG_OUT:
            return {};
        default:
            return state;
    }
}
