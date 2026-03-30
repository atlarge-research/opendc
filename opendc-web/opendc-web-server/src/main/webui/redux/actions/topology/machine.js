export const DELETE_MACHINE = 'DELETE_MACHINE'
export const ADD_UNIT = 'ADD_UNIT'
export const DELETE_UNIT = 'DELETE_UNIT'

export function deleteMachine(machineId) {
    return {
        type: DELETE_MACHINE,
        machineId,
    }
}

export function addUnit(machineId, unitType, unitId) {
    return {
        type: ADD_UNIT,
        machineId,
        unitType,
        unitId,
    }
}

export function deleteUnit(machineId, unitType, unitId) {
    return {
        type: DELETE_UNIT,
        machineId,
        unitType,
        unitId,
    }
}
