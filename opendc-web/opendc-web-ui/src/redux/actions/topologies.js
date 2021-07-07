export const ADD_TOPOLOGY = 'ADD_TOPOLOGY'

export function addTopology(projectId, name, duplicateId) {
    return {
        type: ADD_TOPOLOGY,
        projectId,
        name,
        duplicateId,
    }
}
