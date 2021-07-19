import { call, put, select, getContext } from 'redux-saga/effects'
import { addToStore } from '../actions/objects'
import { addPrefab } from '../../api/prefabs'
import { Rack } from '../../util/topology-schema'
import { denormalize } from 'normalizr'

export function* onAddPrefab({ name, tileId }) {
    try {
        const objects = yield select((state) => state.objects)
        const rack = objects.rack[objects.tile[tileId].rack]
        const prefabRack = denormalize(rack, Rack, objects)
        const auth = yield getContext('auth')
        const prefab = yield call(() => addPrefab(auth, { name, rack: prefabRack }))
        yield put(addToStore('prefab', prefab))
    } catch (error) {
        console.error(error)
    }
}
