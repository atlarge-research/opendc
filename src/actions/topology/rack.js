import {goDownOneInteractionLevel} from "../interaction-level";
import {addPropToStoreObject} from "../objects";

export const EDIT_RACK_NAME = "EDIT_RACK_NAME";
export const DELETE_RACK = "DELETE_RACK";
export const ADD_MACHINE = "ADD_MACHINE";

export function editRackName(name) {
    return {
        type: EDIT_RACK_NAME,
        name
    };
}

export function editRackNameSucceeded(name) {
    return (dispatch, getState) => {
        const {objects, interactionLevel} = getState();
        dispatch(addPropToStoreObject("rack", objects.tile[interactionLevel.tileId].objectId, {name}));
    };
}

export function deleteRack() {
    return {
        type: DELETE_RACK
    };
}

export function deleteRackSucceeded() {
    return (dispatch, getState) => {
        const {interactionLevel} = getState();
        const currentTileId = interactionLevel.tileId;
        dispatch(goDownOneInteractionLevel());
        dispatch(addPropToStoreObject("tile", currentTileId, {objectType: undefined}));
        dispatch(addPropToStoreObject("tile", currentTileId, {objectId: undefined}));
    };
}

export function addMachine(position) {
    return {
        type: ADD_MACHINE,
        position
    };
}

export function addMachineSucceeded(machine) {
    return (dispatch, getState) => {
        const {objects, interactionLevel} = getState();
        const rack = objects.rack[objects.tile[interactionLevel.tileId].objectId];
        const machineIds = [...rack.machineIds];
        machineIds[machine.position - 1] = machine.id;
        dispatch(addPropToStoreObject("rack", rack.id, {machineIds}));
    };
}
