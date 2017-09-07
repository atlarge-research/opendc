import {LOG_IN_SUCCEEDED, LOG_OUT} from "../actions/auth";
import {DELETE_CURRENT_USER_SUCCEEDED} from "../actions/users";

const getAuthObject = () => {
    const authItem = localStorage.getItem("auth");
    if (!authItem || authItem === "{}") {
        return undefined;
    }
    return JSON.parse(authItem);
};

export const userIsLoggedIn = () => {
    const authObj = getAuthObject();

    if (!authObj || !authObj.googleId) {
        return false;
    }

    const currentTime = (new Date()).getTime();
    return parseInt(authObj.expiresAt, 10) - currentTime > 0;
};

export const getAuthToken = () => {
    const authObj = getAuthObject();
    if (!authObj) {
        return undefined;
    }

    return authObj.authToken;
};

export const saveAuthLocalStorage = (payload) => {
    localStorage.setItem("auth", JSON.stringify(payload));
};

export const clearAuthLocalStorage = () => {
    localStorage.setItem("auth", "");
};

export const authRedirectMiddleware = store => next => action => {
    switch (action.type) {
        case LOG_IN_SUCCEEDED:
            saveAuthLocalStorage(action.payload);
            window.location.href = "/simulations";
            break;
        case LOG_OUT:
        case DELETE_CURRENT_USER_SUCCEEDED:
            clearAuthLocalStorage();
            window.location.href = "/";
            break;
        default:
            next(action);
            return;
    }

    next(action);
};
