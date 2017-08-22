import {sendRequest} from "../index";

export function getAll(path) {
    return sendRequest({
        path,
        method: "GET",
        parameters: {
            body: {},
            path: {},
            query: {}
        }
    });
}

export function getById(path, pathObject) {
    return sendRequest({
        path,
        method: "GET",
        parameters: {
            body: {},
            path: pathObject,
            query: {}
        }
    });
}

export function deleteById(path, pathObject) {
    return sendRequest({
        path,
        method: "DELETE",
        parameters: {
            body: {},
            path: pathObject,
            query: {}
        }
    });
}
