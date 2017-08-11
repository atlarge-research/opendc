import {COMPLETE_LOGIN, LOG_OUT} from "../actions/auth";

export const auth = (state = {}, action) => {
    switch (action.type) {
        case COMPLETE_LOGIN:
            return action.payload;
        case LOG_OUT:
            return {};
        default:
            return state;
    }
};
