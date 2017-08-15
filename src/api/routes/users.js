import {sendRequest} from "../index";

export function getUserByEmail(email) {
    return sendRequest({
        path: "/users",
        method: "GET",
        parameters: {
            body: {},
            path: {},
            query: {
                email
            }
        }
    });
}

export function addUser(user) {
    return sendRequest({
        path: "/users",
        method: "POST",
        parameters: {
            body: {
                user: user
            },
            path: {},
            query: {}
        }
    });
}

export function getUser(userId) {
    return sendRequest({
        path: "/users/{userId}",
        method: "GET",
        parameters: {
            body: {},
            path: {
                userId
            },
            query: {}
        }
    });
}

export function updateUser(userId, user) {
    return sendRequest({
        path: "/users/{userId}",
        method: "PUT",
        parameters: {
            body: {
                user: {
                    givenName: user.givenName,
                    familyName: user.familyName
                }
            },
            path: {
                userId
            },
            query: {}
        }
    });
}

export function deleteUser(userId) {
    return sendRequest({
        path: "/users/{userId}",
        method: "DELETE",
        parameters: {
            body: {},
            path: {
                userId
            },
            query: {}
        }
    });
}

export function getAuthorizationsByUser(userId) {
    return sendRequest({
        path: "/users/{userId}/authorizations",
        method: "GET",
        parameters: {
            body: {},
            path: {
                userId
            },
            query: {}
        }
    });
}
