import {findTileWithPosition} from "../../util/tile-calculations";
import {goDownOneInteractionLevel} from "../interaction-level";
import {addPropToStoreObject, removeIdFromStoreObjectListProp} from "../objects";

export const EDIT_ROOM_NAME = "EDIT_ROOM_NAME";
export const DELETE_ROOM = "DELETE_ROOM";
export const START_RACK_CONSTRUCTION = "START_RACK_CONSTRUCTION";
export const STOP_RACK_CONSTRUCTION = "STOP_RACK_CONSTRUCTION";
export const ADD_RACK_TO_TILE = "ADD_RACK_TO_TILE";

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
