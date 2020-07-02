export const ADD_TOPOLOGY = 'ADD_TOPOLOGY'
export const DELETE_TOPOLOGY = 'DELETE_TOPOLOGY'

export function addTopology(topology) {
    return {
        type: ADD_TOPOLOGY,
        topology,
    }
}

export function deleteTopology(id) {
    return {
        type: DELETE_TOPOLOGY,
        id,
    }
}
