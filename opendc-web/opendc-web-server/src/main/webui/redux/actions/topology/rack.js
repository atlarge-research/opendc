import { v4 as uuid } from 'uuid'

export const EDIT_RACK_NAME = 'EDIT_RACK_NAME'
export const EDIT_RACK_CLUSTER_NAME = 'EDIT_RACK_CLUSTER_NAME'
export const EDIT_RACK_POWER_CAPACITY = 'EDIT_RACK_POWER_CAPACITY'
export const DELETE_RACK = 'DELETE_RACK'
export const ADD_MACHINE = 'ADD_MACHINE'

export function editRackName(rackId, name) {
    return {
        type: EDIT_RACK_NAME,
        name,
        rackId,
    }
}

export function editRackPowerCapacity(rackId, powerCapacityW) {
    return {
        type: EDIT_RACK_POWER_CAPACITY,
        powerCapacityW,
        rackId,
    }
}

export function editRackClusterName(rackId, clusterName) {
    return {
        type: EDIT_RACK_CLUSTER_NAME,
        clusterName,
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

export function addMachine(rackId, position, prefab = null) {
    return {
        type: ADD_MACHINE,
        machine: {
            id: uuid(),
            rackId,
            position,
            name: prefab?.name ?? `Machine at position ${position}`,
            cpus: prefab?.cpus?.map((u) => u.id) ?? [],
            gpus: prefab?.gpus?.map((u) => u.id) ?? [],
            memories: prefab?.memories?.map((u) => u.id) ?? [],
            storages: prefab?.storages?.map((u) => u.id) ?? [],
        },
    }
}
