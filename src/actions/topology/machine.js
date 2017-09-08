import {goDownOneInteractionLevel} from "../interaction-level";
import {addPropToStoreObject} from "../objects";

export const DELETE_MACHINE = "DELETE_MACHINE";
export const ADD_UNIT = "ADD_UNIT";
export const DELETE_UNIT = "DELETE_UNIT";

export function deleteMachine() {
    return {
        type: DELETE_MACHINE
    };
}

export function deleteMachineSucceeded() {
    return (dispatch, getState) => {
        const {interactionLevel, objects} = getState();
        const rack = objects.rack[objects.tile[interactionLevel.tileId].objectId];
        const machineIds = [...rack.machineIds];
        machineIds[interactionLevel.position - 1] = null;
        dispatch(goDownOneInteractionLevel());
        dispatch(addPropToStoreObject("rack", rack.id, {machineIds}));
    };
}

export function addUnit(unitType, id) {
    return {
        type: ADD_UNIT,
        unitType,
        id
    };
}

export function addUnitSucceeded(unitType, id) {
    return (dispatch, getState) => {
        const {objects, interactionLevel} = getState();
        const machine = objects.machine[objects.rack[objects.tile[interactionLevel.tileId].objectId]
            .machineIds[interactionLevel.position - 1]];
        const units = [...machine[unitType + "Ids"], id];
        dispatch(addPropToStoreObject("machine", machine.id, {[unitType + "Ids"]: units}));
    };
}

export function deleteUnit(unitType, index) {
    return {
        type: DELETE_UNIT,
        unitType,
        index
    };
}

export function deleteUnitSucceeded(unitType, index) {
    return (dispatch, getState) => {
        const {objects, interactionLevel} = getState();
        const machine = objects.machine[objects.rack[objects.tile[interactionLevel.tileId].objectId]
            .machineIds[interactionLevel.position - 1]];
        const unitIds = machine[unitType + "Ids"].slice();
        unitIds.splice(index, 1);
        dispatch(addPropToStoreObject("machine", machine.id, {[unitType + "Ids"]: unitIds}));
    };
}
