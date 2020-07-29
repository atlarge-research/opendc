import {call, put, select} from "redux-saga/effects";
import {addToStore} from "../actions/objects";
import {addPrefab} from "../api/routes/prefabs";
import {getRackById} from "./objects";

export function* onAddPrefab(action) {
    try {
        const currentRackId = yield select((state) => state.objects.tile[state.interactionLevel.tileId].rackId)
        const currentRackJson = yield getRackById(currentRackId, false)
        const prefab = yield call(addPrefab, { name: action.name, rack: currentRackJson })
        yield put(addToStore('prefab', prefab))

    } catch (error) {
        console.error(error)
    }
}
