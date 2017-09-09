export const EDIT_RACK_NAME = "EDIT_RACK_NAME";
export const DELETE_RACK = "DELETE_RACK";
export const ADD_MACHINE = "ADD_MACHINE";

export function editRackName(name) {
    return {
        type: EDIT_RACK_NAME,
        name
    };
}

export function deleteRack() {
    return {
        type: DELETE_RACK
    };
}

export function addMachine(position) {
    return {
        type: ADD_MACHINE,
        position
    };
}
