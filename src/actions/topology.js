import {addIdToStoreObjectListProp, removeIdFromStoreObjectListProp} from "./objects";

export const FETCH_TOPOLOGY_OF_DATACENTER = "FETCH_TOPOLOGY_OF_DATACENTER";
export const FETCH_TOPOLOGY_OF_DATACENTER_SUCCEEDED = "FETCH_TOPOLOGY_OF_DATACENTER_SUCCEEDED";
export const FETCH_LATEST_DATACENTER = "FETCH_LATEST_DATACENTER";
export const FETCH_LATEST_DATACENTER_SUCCEEDED = "FETCH_LATEST_DATACENTER_SUCCEEDED";
export const START_NEW_ROOM_CONSTRUCTION = "START_NEW_ROOM_CONSTRUCTION";
export const START_NEW_ROOM_CONSTRUCTION_SUCCEEDED = "START_NEW_ROOM_CONSTRUCTION_SUCCEEDED";
export const FINISH_NEW_ROOM_CONSTRUCTION = "FINISH_NEW_ROOM_CONSTRUCTION";
export const CANCEL_NEW_ROOM_CONSTRUCTION = "CANCEL_NEW_ROOM_CONSTRUCTION";
export const CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED = "CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED";
export const ADD_TILE = "ADD_TILE";
export const ADD_TILE_SUCCEEDED = "ADD_TILE_SUCCEEDED";
export const DELETE_TILE = "DELETE_TILE";
export const DELETE_TILE_SUCCEEDED = "DELETE_TILE_SUCCEEDED";

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
        const {objects, currentRoomInConstruction} = getState();
        if (objects.room[currentRoomInConstruction].tileIds.length === 0) {
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
        const {currentDatacenterId, currentRoomInConstruction} = getState();
        dispatch(removeIdFromStoreObjectListProp("datacenter", currentDatacenterId, "roomIds",
            currentRoomInConstruction));
        dispatch({
            type: CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED
        });
    };
}

export function toggleTileAtLocation(positionX, positionY) {
    return (dispatch, getState) => {
        const {objects, currentRoomInConstruction} = getState();

        const tileIds = objects.room[currentRoomInConstruction].tileIds;
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
        const {currentRoomInConstruction} = getState();
        dispatch(addIdToStoreObjectListProp("room", currentRoomInConstruction, "tileIds", tileId));
        dispatch({
            type: ADD_TILE_SUCCEEDED,
            tileId
        });
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
        const {currentRoomInConstruction} = getState();
        dispatch(removeIdFromStoreObjectListProp("room", currentRoomInConstruction, "tileIds", tileId));
        dispatch({
            type: DELETE_TILE_SUCCEEDED,
            tileId
        });
    };
}
