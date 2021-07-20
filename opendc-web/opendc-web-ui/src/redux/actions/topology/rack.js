export const EDIT_RACK_NAME = 'EDIT_RACK_NAME'
export const DELETE_RACK = 'DELETE_RACK'
export const ADD_MACHINE = 'ADD_MACHINE'

export function editRackName(rackId, name) {
    return {
        type: EDIT_RACK_NAME,
        name,
        rackId,
    }
}

export function deleteRack(tileId) {
    return {
        type: DELETE_RACK,
        tileId,
    }
}

export function addMachine(rackId, position) {
    return {
        type: ADD_MACHINE,
        position,
        rackId,
    }
}
