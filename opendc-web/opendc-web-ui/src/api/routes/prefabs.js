import { request } from '../index'

export function getPrefab(prefabId) {
    return request(`prefabs/${prefabId}`)
}

export function addPrefab(prefab) {
    return request('prefabs', 'POST', { prefab })
}

export function updatePrefab(prefab) {
    return request(`prefabs/${prefab._id}`, 'PUT', { prefab })
}

export function deletePrefab(prefabId) {
    return request(`prefabs/${prefabId}`, 'DELETE')
}
