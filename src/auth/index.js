import {COMPLETE_LOGIN, LOG_OUT} from "../actions/auth";

const getAuthObject = () => {
    const authItem = localStorage.getItem("auth");
    if (!authItem) {
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
    localStorage.setItem("auth", "{}");
};

export const authRedirectMiddleware = store => next => action => {
    switch (action.type) {
        case COMPLETE_LOGIN:
            saveAuthLocalStorage(action.payload);
            window.location.href = "/projects";
            break;
        case LOG_OUT:
            clearAuthLocalStorage();
            window.location.href = "/";
            break;
        default:
            next(action);
            return;
    }

    next(action);
};
