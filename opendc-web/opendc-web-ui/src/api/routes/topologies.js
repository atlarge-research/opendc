import { request } from '../index'

export function addTopology(topology) {
    return request(`projects/${topology.projectId}/topologies`, 'POST', { topology })
}

export function getTopology(topologyId) {
    return request(`topologies/${topologyId}`)
}

export function updateTopology(topology) {
    return request(`topologies/${topology._id}`, 'PUT', { topology })
}

export function deleteTopology(topologyId) {
    return request(`topologies/${topologyId}`, 'DELETE')
}
