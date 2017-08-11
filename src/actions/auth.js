export const COMPLETE_LOGIN = "COMPLETE_LOGIN";
export const LOG_OUT = "LOG_OUT";

export const completeLogin = (payload) => {
    return {
        type: COMPLETE_LOGIN,
        payload
    };
};

export const logOut = () => {
    return {
        type: LOG_OUT
    };
};
