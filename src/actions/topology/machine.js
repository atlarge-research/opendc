export const DELETE_MACHINE = "DELETE_MACHINE";
export const ADD_UNIT = "ADD_UNIT";
export const DELETE_UNIT = "DELETE_UNIT";

export function deleteMachine() {
    return {
        type: DELETE_MACHINE
    };
}

export function addUnit(unitType, id) {
    return {
        type: ADD_UNIT,
        unitType,
        id
    };
}

export function deleteUnit(unitType, index) {
    return {
        type: DELETE_UNIT,
        unitType,
        index
    };
}
