import {findTileWithPosition} from "../util/tile-calculations";
import {goDownOneInteractionLevel} from "./interaction-level";
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
export const DELETE_ROOM = "DELETE_ROOM";
export const EDIT_RACK_NAME = "EDIT_RACK_NAME";
export const DELETE_RACK = "DELETE_RACK";
export const START_RACK_CONSTRUCTION = "START_RACK_CONSTRUCTION";
export const STOP_RACK_CONSTRUCTION = "STOP_RACK_CONSTRUCTION";
export const ADD_RACK_TO_TILE = "ADD_RACK_TO_TILE";
export const ADD_MACHINE = "ADD_MACHINE";
export const DELETE_MACHINE = "DELETE_MACHINE";

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

export function startRackConstruction() {
    return {
        type: START_RACK_CONSTRUCTION
    };
}

export function stopRackConstruction() {
    return {
        type: STOP_RACK_CONSTRUCTION
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

export function deleteRoom() {
    return {
        type: DELETE_ROOM
    };
}

export function deleteRoomSucceeded() {
    return (dispatch, getState) => {
        const {currentDatacenterId, interactionLevel} = getState();
        const currentRoomId = interactionLevel.roomId;
        dispatch(goDownOneInteractionLevel());
        dispatch(removeIdFromStoreObjectListProp("datacenter", currentDatacenterId, "roomIds", currentRoomId));
    };
}

export function editRackName(name) {
    return {
        type: EDIT_RACK_NAME,
        name
    };
}

export function editRackNameSucceeded(name) {
    return (dispatch, getState) => {
        const {objects, interactionLevel} = getState();
        dispatch(addPropToStoreObject("rack", objects.tile[interactionLevel.tileId].objectId, {name}));
    };
}

export function deleteRack() {
    return {
        type: DELETE_RACK
    };
}

export function deleteRackSucceeded() {
    return (dispatch, getState) => {
        const {interactionLevel} = getState();
        const currentTileId = interactionLevel.tileId;
        dispatch(goDownOneInteractionLevel());
        dispatch(addPropToStoreObject("tile", currentTileId, {objectType: undefined}));
        dispatch(addPropToStoreObject("tile", currentTileId, {objectId: undefined}));
    };
}

export function addMachine(position) {
    return {
        type: ADD_MACHINE,
        position
    };
}

export function addMachineSucceeded(machine) {
    return (dispatch, getState) => {
        const {objects, interactionLevel} = getState();
        const rack = objects.rack[objects.tile[interactionLevel.tileId].objectId];
        const machineIds = [...rack.machineIds];
        machineIds[machine.position - 1] = machine.id;
        dispatch(addPropToStoreObject("rack", rack.id, {machineIds}));
    };
}

export function deleteMachine() {
    return {
        type: DELETE_MACHINE
    };
}

export function deleteMachineSucceeded() {
    return (dispatch, getState) => {
        const {interactionLevel, objects} = getState();
        const rack = objects.rack[objects.tile[interactionLevel.tileId].objectId];
        const machineIds = [...rack.machineIds];
        machineIds[interactionLevel.position - 1] = null;
        dispatch(goDownOneInteractionLevel());
        dispatch(addPropToStoreObject("rack", rack.id, {machineIds}));
    };
}
