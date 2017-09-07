import {goDownOneInteractionLevel} from "../interaction-level";
import {addPropToStoreObject} from "../objects";

export const DELETE_MACHINE = "DELETE_MACHINE";

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
