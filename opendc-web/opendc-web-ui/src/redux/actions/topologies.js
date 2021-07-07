export const ADD_TOPOLOGY = 'ADD_TOPOLOGY'
export const DELETE_TOPOLOGY = 'DELETE_TOPOLOGY'

export function addTopology(projectId, name, duplicateId) {
    return {
        type: ADD_TOPOLOGY,
        projectId,
        name,
        duplicateId,
    }
}

export function deleteTopology(id) {
    return {
        type: DELETE_TOPOLOGY,
        id,
    }
}
