import { QueryObserver, MutationObserver } from 'react-query'
import { normalize, denormalize } from 'normalizr'
import { select, put, take, race, getContext, call } from 'redux-saga/effects'
import { eventChannel } from 'redux-saga'
import { Topology } from '../../util/topology-schema'
import { storeTopology, OPEN_TOPOLOGY } from '../actions/topology'

/**
 * Update the topology on the server.
 */
export function* updateServer() {
    const queryClient = yield getContext('queryClient')
    const mutationObserver = new MutationObserver(queryClient, { mutationKey: 'updateTopology' })

    while (true) {
        yield take(
            (action) =>
                action.type.startsWith('EDIT') || action.type.startsWith('ADD') || action.type.startsWith('DELETE')
        )
        const topology = yield select((state) => state.topology)

        if (!topology.root) {
            continue
        }

        const denormalizedTopology = denormalize(topology.root, Topology, topology)
        yield call([mutationObserver, mutationObserver.mutate], denormalizedTopology)
    }
}

/**
 * Watch the topology on the server for changes.
 */
export function* watchServer() {
    let { projectId, id } = yield take(OPEN_TOPOLOGY)
    while (true) {
        const channel = yield queryObserver(projectId, id)

        while (true) {
            const [action, response] = yield race([take(OPEN_TOPOLOGY), take(channel)])

            if (action) {
                projectId = action.projectId
                id = action.id
                break
            }

            const { isFetched, data } = response
            // Only update the topology on the client-side when a new topology was fetched
            if (isFetched) {
                const { result: topologyId, entities } = normalize(data, Topology)
                yield put(storeTopology(entities.topologies[topologyId], entities))
            }
        }
    }
}

/**
 * Observe changes for the topology with the specified identifier.
 */
function* queryObserver(projectId, id) {
    const queryClient = yield getContext('queryClient')
    const observer = new QueryObserver(queryClient, { queryKey: ['topologies', projectId, id] })

    return eventChannel((emitter) => {
        const unsubscribe = observer.subscribe((result) => {
            emitter(result)
        })

        // Update result to make sure we did not miss any query updates
        // between creating the observer and subscribing to it.
        observer.updateResult()

        return unsubscribe
    })
}
