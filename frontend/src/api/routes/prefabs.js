import { sendRequest } from '../index'
import { deleteById, getById } from './util'

export function getPrefab(prefabId) {
    return getById('/prefabs/{prefabId}', { prefabId })
}

export function addPrefab(prefab) {
    return sendRequest({
        path: '/prefabs',
        method: 'POST',
        parameters: {
            body: {
                prefab,
            },
            path: {},
            query: {},
        },
    })
}

export function updatePrefab(prefab) {
    return sendRequest({
        path: '/prefabs/{prefabId}',
        method: 'PUT',
        parameters: {
            body: {
                prefab,
            },
            path: {
                prefabId: prefab._id,
            },
            query: {},
        },
    })
}

export function deletePrefab(prefabId) {
    return deleteById('/prefabs/{prefabId}', { prefabId })
}
