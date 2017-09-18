import {deleteById, getById} from "./util";

export function getExperiment(experimentId) {
    return getById("/experiments/{experimentId}", {experimentId});
}

export function deleteExperiment(experimentId) {
    return deleteById("/experiments/{experimentId}", {experimentId});
}

export function getLastSimulatedTick(experimentId) {
    return getById("/experiments/{experimentId}/last-simulated-tick", {experimentId});
}

export function getAllMachineStates(experimentId) {
    return getById("/experiments/{experimentId}/machine-states", {experimentId});
}

export function getAllRackStates(experimentId) {
    return getById("/experiments/{experimentId}/rack-states", {experimentId});
}

export function getAllRoomStates(experimentId) {
    return getById("/experiments/{experimentId}/room-states", {experimentId});
}

export function getAllTaskStates(experimentId) {
    return getById("/experiments/{experimentId}/task-states", {experimentId});
}
