export const GO_FROM_BUILDING_TO_DATACENTER = 'GO_FROM_BUILDING_TO_DATACENTER'
export const GO_FROM_DATACENTER_TO_ROOM = 'GO_FROM_DATACENTER_TO_ROOM'
export const GO_FROM_ROOM_TO_RACK = 'GO_FROM_ROOM_TO_RACK'
export const GO_FROM_RACK_TO_MACHINE = 'GO_FROM_RACK_TO_MACHINE'
export const GO_DOWN_ONE_INTERACTION_LEVEL = 'GO_DOWN_ONE_INTERACTION_LEVEL'

export function goFromBuildingToDatacenter(datacenterId) {
    return (dispatch, getState) => {
        const { interactionLevel } = getState()
        if (interactionLevel.mode !== 'BUILDING') {
            return
        }

        dispatch({
            type: GO_FROM_BUILDING_TO_DATACENTER,
            datacenterId,
        })
    }
}

export function goFromDatacenterToRoom(roomId) {
    return (dispatch, getState) => {
        const { interactionLevel } = getState()
        if (interactionLevel.mode !== 'DATACENTER') {
            return
        }

        dispatch({
            type: GO_FROM_DATACENTER_TO_ROOM,
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

/**
 * Navigate directly to a room by looking up its datacenter in the Redux store.
 */
export function goToRoom(roomId) {
    return (dispatch, getState) => {
        const { topology } = getState()
        const room = topology.rooms[roomId]
        if (!room) return

        dispatch({
            type: GO_FROM_BUILDING_TO_DATACENTER,
            datacenterId: room.datacenterId,
        })
        dispatch({
            type: GO_FROM_DATACENTER_TO_ROOM,
            roomId,
        })
    }
}
