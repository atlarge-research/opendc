import { call, put, select, getContext } from 'redux-saga/effects'
import { fetchTopology, updateTopology } from '../../api/topologies'
import { Topology } from '../../util/topology-schema'
import { denormalize, normalize } from 'normalizr'
import { storeTopology } from '../actions/topologies'

/**
 * Fetches and normalizes the topology with the specified identifier.
 */
export const fetchAndStoreTopology = function* (id) {
    const auth = yield getContext('auth')

    let topology = yield select((state) => state.objects.topology[id])
    if (!topology) {
        const newTopology = yield call(fetchTopology, auth, id)
        const { entities } = normalize(newTopology, Topology)
        yield put(storeTopology(entities))
    }

    return topology
}

export const updateTopologyOnServer = function* (id) {
    const topology = yield denormalizeTopology(id)
    const auth = yield getContext('auth')
    yield call(updateTopology, auth, topology)
}

/**
 * Denormalizes the topology representation in order to be stored on the server.
 */
export const denormalizeTopology = function* (id) {
    const objects = yield select((state) => state.objects)
    const topology = objects.topology[id]
    return denormalize(topology, Topology, objects)
}
