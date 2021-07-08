import { findTileWithPosition } from '../../../util/tile-calculations'

export const EDIT_ROOM_NAME = 'EDIT_ROOM_NAME'
export const DELETE_ROOM = 'DELETE_ROOM'
export const START_RACK_CONSTRUCTION = 'START_RACK_CONSTRUCTION'
export const STOP_RACK_CONSTRUCTION = 'STOP_RACK_CONSTRUCTION'
export const ADD_RACK_TO_TILE = 'ADD_RACK_TO_TILE'

export function editRoomName(name) {
    return {
        type: EDIT_ROOM_NAME,
        name,
    }
}

export function startRackConstruction() {
    return {
        type: START_RACK_CONSTRUCTION,
    }
}

export function stopRackConstruction() {
    return {
        type: STOP_RACK_CONSTRUCTION,
    }
}

export function addRackToTile(positionX, positionY) {
    return (dispatch, getState) => {
        const { objects, interactionLevel } = getState()
        const currentRoom = objects.room[interactionLevel.roomId]
        const tiles = currentRoom.tiles.map((tileId) => objects.tile[tileId])
        const tile = findTileWithPosition(tiles, positionX, positionY)

        if (tile !== null) {
            dispatch({
                type: ADD_RACK_TO_TILE,
                tileId: tile._id,
            })
        }
    }
}

export function deleteRoom() {
    return {
        type: DELETE_ROOM,
    }
}
