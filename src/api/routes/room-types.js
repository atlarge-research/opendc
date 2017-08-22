import {getAll, getById} from "./util";

export function getAvailableRoomTypes() {
    return getAll("/room-types");
}

export function getAllowedObjectsOfRoomType(name) {
    return getById("/room-types/{name}/allowed-objects", {name});
}
