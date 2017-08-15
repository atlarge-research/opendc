export const LOG_IN = "LOG_IN";
export const LOG_IN_SUCCEEDED = "LOG_IN_SUCCEEDED";
export const LOG_OUT = "LOG_OUT";

export function logIn(payload) {
    return {
        type: LOG_IN,
        payload
    };
}

export function logInSucceeded(payload) {
    return {
        type: LOG_IN_SUCCEEDED,
        payload
    };
}

export function logOut() {
    return {
        type: LOG_OUT
    };
}
