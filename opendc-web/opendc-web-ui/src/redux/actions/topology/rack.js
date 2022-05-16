import { v4 as uuid } from 'uuid'

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

export function deleteRack(tileId, rackId) {
    return {
        type: DELETE_RACK,
        rackId,
        tileId,
    }
}

export function addMachine(rackId, position) {
    return {
        type: ADD_MACHINE,
        machine: {
            id: uuid(),
            rackId,
            position,
            cpus: [],
            gpus: [],
            memories: [],
            storages: [],
        },
    }
}
