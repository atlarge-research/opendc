import { call, put, select, getContext } from 'redux-saga/effects'
import { addToStore } from '../actions/objects'
import { addPrefab } from '../../api/prefabs'
import { getRackById } from './objects'

export function* onAddPrefab(action) {
    try {
        const currentRackId = yield select((state) => state.objects.tile[state.interactionLevel.tileId].rackId)
        const currentRackJson = yield getRackById(currentRackId, false)
        const auth = yield getContext('auth')
        const prefab = yield call(addPrefab, auth, { name: action.name, rack: currentRackJson })
        yield put(addToStore('prefab', prefab))
    } catch (error) {
        console.error(error)
    }
}
