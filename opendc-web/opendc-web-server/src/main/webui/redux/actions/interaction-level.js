export const GO_FROM_BUILDING_TO_ROOM = 'GO_FROM_BUILDING_TO_ROOM'
export const GO_FROM_ROOM_TO_RACK = 'GO_FROM_ROOM_TO_RACK'
export const GO_FROM_RACK_TO_MACHINE = 'GO_FROM_RACK_TO_MACHINE'
export const GO_DOWN_ONE_INTERACTION_LEVEL = 'GO_DOWN_ONE_INTERACTION_LEVEL'

export function goToRoom(roomId) {
    return {
        type: GO_FROM_BUILDING_TO_ROOM,
        roomId,
    }
}

export function goFromBuildingToRoom(roomId) {
    return (dispatch, getState) => {
        const { interactionLevel } = getState()
        if (interactionLevel.mode !== 'BUILDING') {
            return
        }

        dispatch({
            type: GO_FROM_BUILDING_TO_ROOM,
            roomId,
        })
    }
}

export function goFromRoomToRack(tileId) {
    return (dispatch, getState) => {
        const { interactionLevel } = getState()
        if (interactionLevel.mode !== 'ROOM') {
            return
        }
        dispatch({
            type: GO_FROM_ROOM_TO_RACK,
            tileId,
        })
    }
}

export function goFromRackToMachine(position) {
    return (dispatch, getState) => {
        const { interactionLevel } = getState()
        if (interactionLevel.mode !== 'RACK') {
            return
        }
        dispatch({
            type: GO_FROM_RACK_TO_MACHINE,
            position,
        })
    }
}

export function goDownOneInteractionLevel() {
    return {
        type: GO_DOWN_ONE_INTERACTION_LEVEL,
    }
}
