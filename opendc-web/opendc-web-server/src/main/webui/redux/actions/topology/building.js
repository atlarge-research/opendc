import { v4 as uuid } from 'uuid'
import { addRoom, deleteRoom } from './room'

export const START_NEW_ROOM_CONSTRUCTION = 'START_NEW_ROOM_CONSTRUCTION'
export const START_NEW_ROOM_CONSTRUCTION_SUCCEEDED = 'START_NEW_ROOM_CONSTRUCTION_SUCCEEDED'
export const FINISH_NEW_ROOM_CONSTRUCTION = 'FINISH_NEW_ROOM_CONSTRUCTION'
export const CANCEL_NEW_ROOM_CONSTRUCTION = 'CANCEL_NEW_ROOM_CONSTRUCTION'
export const CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED = 'CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED'
export const START_ROOM_EDIT = 'START_ROOM_EDIT'
export const FINISH_ROOM_EDIT = 'FINISH_ROOM_EDIT'
export const ADD_TILE = 'ADD_TILE'
export const DELETE_TILE = 'DELETE_TILE'

export function startNewRoomConstruction() {
    return (dispatch, getState) => {
        const { topology } = getState()
        const topologyId = topology.root.id
        const room = {
            id: uuid(),
            name: 'Room',
            topologyId,
            tiles: [],
        }

        dispatch(addRoom(topologyId, room))
        dispatch(startNewRoomConstructionSucceeded(room.id))
    }
}

export function startNewRoomConstructionSucceeded(roomId) {
    return {
        type: START_NEW_ROOM_CONSTRUCTION_SUCCEEDED,
        roomId,
    }
}

export function finishNewRoomConstruction() {
    return (dispatch, getState) => {
        const { topology, construction } = getState()
        if (topology.rooms[construction.currentRoomInConstruction].tiles.length === 0) {
            dispatch(cancelNewRoomConstruction())
            return
        }

        dispatch({
            type: FINISH_NEW_ROOM_CONSTRUCTION,
        })
    }
}

export function cancelNewRoomConstruction() {
    return (dispatch, getState) => {
        const { construction } = getState()
        const roomId = construction.currentRoomInConstruction
        dispatch(deleteRoom(roomId))
        dispatch(cancelNewRoomConstructionSucceeded())
    }
}

export function cancelNewRoomConstructionSucceeded() {
    return {
        type: CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED,
    }
}

export function startRoomEdit(roomId) {
    return {
        type: START_ROOM_EDIT,
        roomId: roomId,
    }
}

export function finishRoomEdit() {
    return {
        type: FINISH_ROOM_EDIT,
    }
}

export function toggleTileAtLocation(positionX, positionY) {
    return (dispatch, getState) => {
        const { topology, construction } = getState()

        const roomId = construction.currentRoomInConstruction
        const tileIds = topology.rooms[roomId].tiles
        for (const tileId of tileIds) {
            if (topology.tiles[tileId].positionX === positionX && topology.tiles[tileId].positionY === positionY) {
                dispatch(deleteTile(tileId))
                return
            }
        }

        dispatch(addTile(roomId, positionX, positionY))
    }
}

export function addTile(roomId, positionX, positionY) {
    return {
        type: ADD_TILE,
        tile: {
            id: uuid(),
            roomId,
            positionX,
            positionY,
        },
    }
}

export function deleteTile(tileId) {
    return {
        type: DELETE_TILE,
        tileId,
    }
}
