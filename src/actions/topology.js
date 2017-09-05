import {findTileWithPosition} from "../util/tile-calculations";
import {addIdToStoreObjectListProp, addPropToStoreObject, removeIdFromStoreObjectListProp} from "./objects";

export const FETCH_TOPOLOGY_OF_DATACENTER = "FETCH_TOPOLOGY_OF_DATACENTER";
export const FETCH_TOPOLOGY_OF_DATACENTER_SUCCEEDED = "FETCH_TOPOLOGY_OF_DATACENTER_SUCCEEDED";
export const FETCH_LATEST_DATACENTER = "FETCH_LATEST_DATACENTER";
export const FETCH_LATEST_DATACENTER_SUCCEEDED = "FETCH_LATEST_DATACENTER_SUCCEEDED";
export const RESET_CURRENT_DATACENTER = "RESET_CURRENT_DATACENTER";
export const START_NEW_ROOM_CONSTRUCTION = "START_NEW_ROOM_CONSTRUCTION";
export const START_NEW_ROOM_CONSTRUCTION_SUCCEEDED = "START_NEW_ROOM_CONSTRUCTION_SUCCEEDED";
export const FINISH_NEW_ROOM_CONSTRUCTION = "FINISH_NEW_ROOM_CONSTRUCTION";
export const CANCEL_NEW_ROOM_CONSTRUCTION = "CANCEL_NEW_ROOM_CONSTRUCTION";
export const CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED = "CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED";
export const ADD_TILE = "ADD_TILE";
export const DELETE_TILE = "DELETE_TILE";
export const EDIT_ROOM_NAME = "EDIT_ROOM_NAME";
export const START_OBJECT_CONSTRUCTION = "START_OBJECT_CONSTRUCTION";
export const STOP_OBJECT_CONSTRUCTION = "STOP_OBJECT_CONSTRUCTION";
export const ADD_RACK_TO_TILE = "ADD_RACK_TO_TILE";
export const ADD_RACK_TO_TILE_SUCCEEDED = "ADD_RACK_TO_TILE_SUCCEEDED";

export function fetchLatestDatacenter() {
    return (dispatch, getState) => {
        const {currentSimulationId} = getState();
        dispatch({
            type: FETCH_LATEST_DATACENTER,
            currentSimulationId
        });
    };
}

export function fetchLatestDatacenterSucceeded(datacenterId) {
    return {
        type: FETCH_LATEST_DATACENTER_SUCCEEDED,
        datacenterId
    };
}

export function resetCurrentDatacenter() {
    return {
        type: RESET_CURRENT_DATACENTER
    };
}

export function startNewRoomConstruction() {
    return {
        type: START_NEW_ROOM_CONSTRUCTION
    };
}

export function startNewRoomConstructionSucceeded(roomId) {
    return (dispatch, getState) => {
        const {currentDatacenterId} = getState();
        dispatch(addIdToStoreObjectListProp("datacenter", currentDatacenterId, "roomIds", roomId));
        dispatch({
            type: START_NEW_ROOM_CONSTRUCTION_SUCCEEDED,
            roomId
        });
    };
}

export function finishNewRoomConstruction() {
    return (dispatch, getState) => {
        const {objects, construction} = getState();
        if (objects.room[construction.currentRoomInConstruction].tileIds.length === 0) {
            dispatch(cancelNewRoomConstruction());
            return;
        }

        dispatch({
            type: FINISH_NEW_ROOM_CONSTRUCTION
        });
    };
}

export function cancelNewRoomConstruction() {
    return {
        type: CANCEL_NEW_ROOM_CONSTRUCTION
    };
}

export function cancelNewRoomConstructionSucceeded() {
    return (dispatch, getState) => {
        const {currentDatacenterId, construction} = getState();
        dispatch(removeIdFromStoreObjectListProp("datacenter", currentDatacenterId, "roomIds",
            construction.currentRoomInConstruction));
        dispatch({
            type: CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED
        });
    };
}

export function toggleTileAtLocation(positionX, positionY) {
    return (dispatch, getState) => {
        const {objects, construction} = getState();

        const tileIds = objects.room[construction.currentRoomInConstruction].tileIds;
        for (let index in tileIds) {
            if (objects.tile[tileIds[index]].positionX === positionX
                && objects.tile[tileIds[index]].positionY === positionY) {
                dispatch(deleteTile(tileIds[index]));
                return;
            }
        }
        dispatch(addTile(positionX, positionY));
    };
}

export function addTile(positionX, positionY) {
    return {
        type: ADD_TILE,
        positionX,
        positionY
    };
}

export function addTileSucceeded(tileId) {
    return (dispatch, getState) => {
        const {construction} = getState();
        dispatch(addIdToStoreObjectListProp("room", construction.currentRoomInConstruction, "tileIds", tileId));
    };
}

export function deleteTile(tileId) {
    return {
        type: DELETE_TILE,
        tileId
    }
}

export function deleteTileSucceeded(tileId) {
    return (dispatch, getState) => {
        const {construction} = getState();
        dispatch(removeIdFromStoreObjectListProp("room", construction.currentRoomInConstruction, "tileIds", tileId));
    };
}

export function editRoomName(name) {
    return {
        type: EDIT_ROOM_NAME,
        name
    };
}

export function editRoomNameSucceeded(name) {
    return (dispatch, getState) => {
        const {interactionLevel} = getState();
        dispatch(addPropToStoreObject("room", interactionLevel.roomId, {name}));
    };
}

export function startObjectConstruction() {
    return {
        type: START_OBJECT_CONSTRUCTION
    };
}

export function stopObjectConstruction() {
    return {
        type: STOP_OBJECT_CONSTRUCTION
    };
}

export function addRackToTile(positionX, positionY) {
    return (dispatch, getState) => {
        const {objects, interactionLevel} = getState();
        const currentRoom = objects.room[interactionLevel.roomId];
        const tiles = currentRoom.tileIds.map(tileId => objects.tile[tileId]);
        const tile = findTileWithPosition(tiles, positionX, positionY);

        if (tile !== null) {
            dispatch({
                type: ADD_RACK_TO_TILE,
                tileId: tile.id
            });
        }
    };
}

export function addRackToTileSucceeded(tileId, rackId) {
    return dispatch => {
        dispatch(addPropToStoreObject("tile", tileId, {objectType: "RACK"}));
        dispatch(addPropToStoreObject("tile", tileId, {objectId: rackId}));
    };
}
