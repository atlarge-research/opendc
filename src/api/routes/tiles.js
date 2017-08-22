import {sendRequest} from "../index";
import {deleteById, getById} from "./util";

export function getTile(tileId) {
    return getById("/tiles/{tileId}", {tileId});
}

export function deleteTile(tileId) {
    return deleteById("/tiles/{tileId}", {tileId});
}

export function getRackByTile(tileId) {
    return getTileObject(tileId, "/rack");
}

export function addRackToTile(tileId, rack) {
    return addTileObject(tileId, {rack}, "/rack");
}

export function updateRackOnTile(tileId, rack) {
    return updateTileObject(tileId, {rack}, "/rack");
}

export function deleteRackFromTile(tileId) {
    return deleteTileObject(tileId, "/rack");
}

export function getMachinesOfRackByTile(tileId) {
    return getById("/tiles/{tileId}/rack/machines", {tileId});
}

export function addMachineToRackOnTile(tileId, machine) {
    return sendRequest({
        path: "/tiles/{tileId}/rack/machines",
        method: "GET",
        parameters: {
            body: {
                machine
            },
            path: {
                tileId
            },
            query: {}
        }
    });
}

export function updateMachineInRackOnTile(tileId, position, machine) {
    return sendRequest({
        path: "/tiles/{tileId}/rack/machines/{position}",
        method: "PUT",
        parameters: {
            body: {
                machine
            },
            path: {
                tileId,
                position
            },
            query: {}
        }
    });
}

export function deleteMachineInRackOnTile(tileId, position) {
    return sendRequest({
        path: "/tiles/{tileId}/rack/machines/{position}",
        method: "DELETE",
        parameters: {
            body: {},
            path: {
                tileId,
                position
            },
            query: {}
        }
    });
}

export function getCoolingItemByTile(tileId) {
    return getTileObject(tileId, "/cooling-item");
}

export function addCoolingItemToTile(tileId, coolingItemId) {
    return addTileObject(tileId, {coolingItemId}, "/cooling-item");
}

export function updateCoolingItemOnTile(tileId, coolingItemId) {
    return updateTileObject(tileId, {coolingItemId}, "/cooling-item");
}

export function deleteCoolingItemFromTile(tileId) {
    return deleteTileObject(tileId, "/cooling-item");
}

export function getPSUByTile(tileId) {
    return getTileObject(tileId, "/psu");
}

export function addPSUToTile(tileId, psuId) {
    return addTileObject(tileId, {psuId}, "/psu");
}

export function updatePSUOnTile(tileId, psuId) {
    return updateTileObject(tileId, {psuId}, "/psu");
}

export function deletePSUFromTile(tileId) {
    return deleteTileObject(tileId, "/psu");
}

function getTileObject(tileId, endpoint) {
    return getById("/tiles/{tileId}" + endpoint, {tileId});
}

function addTileObject(tileId, objectBody, endpoint) {
    return sendRequest({
        path: "/tiles/{tileId}" + endpoint,
        method: "POST",
        parameters: {
            body: objectBody,
            path: {
                tileId
            },
            query: {}
        }
    });
}

function updateTileObject(tileId, objectBody, endpoint) {
    return sendRequest({
        path: "/tiles/{tileId}" + endpoint,
        method: "PUT",
        parameters: {
            body: objectBody,
            path: {
                tileId
            },
            query: {}
        }
    });
}

function deleteTileObject(tileId, endpoint) {
    return deleteById("/tiles/{tileId}" + endpoint, {tileId});
}
