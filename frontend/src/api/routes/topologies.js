import { deleteById, getById } from './util'
import { sendRequest } from '../index'

export function addTopology(topology) {
    return sendRequest({
        path: '/simulations/{simulationId}/topologies',
        method: 'POST',
        parameters: {
            body: {
                topology,
            },
            path: {
                simulationId: topology.simulationId,
            },
            query: {},
        },
    })
}

export function getTopology(topologyId) {
    return getById('/topologies/{topologyId}', { topologyId })
}

export function updateTopology(topology) {
    return sendRequest({
        path: '/topologies/{topologyId}',
        method: 'PUT',
        parameters: {
            body: {
                topology,
            },
            path: {
                topologyId: topology._id,
            },
            query: {},
        },
    })
}

export function deleteTopology(topologyId) {
    return deleteById('/topologies/{topologyId}', { topologyId })
}
