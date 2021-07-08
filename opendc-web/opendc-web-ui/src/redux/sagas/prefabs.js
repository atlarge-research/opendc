import { call, put, select, getContext } from 'redux-saga/effects'
import { addToStore } from '../actions/objects'
import { addPrefab } from '../../api/prefabs'
import { Rack } from '../../util/topology-schema'
import { denormalize } from 'normalizr'

export function* onAddPrefab(action) {
    try {
        const interactionLevel = yield select((state) => state.interactionLevel)
        const objects = yield select((state) => state.objects)
        const rack = objects.rack[objects.tile[interactionLevel.tileId].rack]
        const prefabRack = denormalize(rack, Rack, objects)
        const auth = yield getContext('auth')
        const prefab = yield call(() => addPrefab(auth, { name: action.name, rack: prefabRack }))
        yield put(addToStore('prefab', prefab))
    } catch (error) {
        console.error(error)
    }
}
