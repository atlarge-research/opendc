export const FETCH_AUTHORIZATIONS_OF_CURRENT_USER = "FETCH_AUTHORIZATIONS_OF_CURRENT_USER";
export const FETCH_AUTHORIZATIONS_OF_CURRENT_USER_SUCCEEDED = "FETCH_AUTHORIZATIONS_OF_CURRENT_USER_SUCCEEDED";

export function fetchAuthorizationsOfCurrentUser() {
    return (dispatch, getState) => {
        const {auth} = getState();
        dispatch({
            type: FETCH_AUTHORIZATIONS_OF_CURRENT_USER,
            userId: auth.userId
        });
    };
}

export function fetchAuthorizationsOfCurrentUserSucceeded(authorizationsOfCurrentUser) {
    return {
        type: FETCH_AUTHORIZATIONS_OF_CURRENT_USER_SUCCEEDED,
        authorizationsOfCurrentUser
    };
}
