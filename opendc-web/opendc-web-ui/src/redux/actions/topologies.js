export const OPEN_TOPOLOGY = 'OPEN_TOPOLOGY'
export const ADD_TOPOLOGY = 'ADD_TOPOLOGY'
export const STORE_TOPOLOGY = 'STORE_TOPOLOGY'

export function openTopology(id) {
    return {
        type: OPEN_TOPOLOGY,
        id,
    }
}

export function addTopology(projectId, name, duplicateId) {
    return {
        type: ADD_TOPOLOGY,
        projectId,
        name,
        duplicateId,
    }
}

export function storeTopology(entities) {
    return {
        type: STORE_TOPOLOGY,
        entities,
    }
}
