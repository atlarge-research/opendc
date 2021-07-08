export const SET_CURRENT_TOPOLOGY = 'SET_CURRENT_TOPOLOGY'
export const START_NEW_ROOM_CONSTRUCTION = 'START_NEW_ROOM_CONSTRUCTION'
export const START_NEW_ROOM_CONSTRUCTION_SUCCEEDED = 'START_NEW_ROOM_CONSTRUCTION_SUCCEEDED'
export const FINISH_NEW_ROOM_CONSTRUCTION = 'FINISH_NEW_ROOM_CONSTRUCTION'
export const CANCEL_NEW_ROOM_CONSTRUCTION = 'CANCEL_NEW_ROOM_CONSTRUCTION'
export const CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED = 'CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED'
export const START_ROOM_EDIT = 'START_ROOM_EDIT'
export const FINISH_ROOM_EDIT = 'FINISH_ROOM_EDIT'
export const ADD_TILE = 'ADD_TILE'
export const DELETE_TILE = 'DELETE_TILE'

export function setCurrentTopology(topologyId) {
    return {
        type: SET_CURRENT_TOPOLOGY,
        topologyId,
    }
}

export function startNewRoomConstruction() {
    return {
        type: START_NEW_ROOM_CONSTRUCTION,
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
        const { objects, construction } = getState()
        if (objects.room[construction.currentRoomInConstruction].tiles.length === 0) {
            dispatch(cancelNewRoomConstruction())
            return
        }

        dispatch({
            type: FINISH_NEW_ROOM_CONSTRUCTION,
        })
    }
}

export function cancelNewRoomConstruction() {
    return {
        type: CANCEL_NEW_ROOM_CONSTRUCTION,
    }
}

export function cancelNewRoomConstructionSucceeded() {
    return {
        type: CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED,
    }
}

export function startRoomEdit() {
    return (dispatch, getState) => {
        const { interactionLevel } = getState()
        dispatch({
            type: START_ROOM_EDIT,
            roomId: interactionLevel.roomId,
        })
    }
}

export function finishRoomEdit() {
    return {
        type: FINISH_ROOM_EDIT,
    }
}

export function toggleTileAtLocation(positionX, positionY) {
    return (dispatch, getState) => {
        const { objects, construction } = getState()

        const tileIds = objects.room[construction.currentRoomInConstruction].tiles
        for (const tileId of tileIds) {
            if (objects.tile[tileId].positionX === positionX && objects.tile[tileId].positionY === positionY) {
                dispatch(deleteTile(tileId))
                return
            }
        }
        dispatch(addTile(positionX, positionY))
    }
}

export function addTile(positionX, positionY) {
    return {
        type: ADD_TILE,
        positionX,
        positionY,
    }
}

export function deleteTile(tileId) {
    return {
        type: DELETE_TILE,
        tileId,
    }
}
