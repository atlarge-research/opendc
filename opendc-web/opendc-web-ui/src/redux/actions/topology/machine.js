export const DELETE_MACHINE = 'DELETE_MACHINE'
export const ADD_UNIT = 'ADD_UNIT'
export const DELETE_UNIT = 'DELETE_UNIT'

export function deleteMachine(rackId, position) {
    return {
        type: DELETE_MACHINE,
        rackId,
        position,
    }
}

export function addUnit(machineId, unitType, id) {
    return {
        type: ADD_UNIT,
        machineId,
        unitType,
        id,
    }
}

export function deleteUnit(machineId, unitType, index) {
    return {
        type: DELETE_UNIT,
        machineId,
        unitType,
        index,
    }
}
