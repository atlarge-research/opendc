import {call, put, select} from "redux-saga/effects";
import {addToStore} from "../actions/objects";
import {addPrefab} from "../api/routes/prefabs";
import {getTopologyAsObject} from "./objects";

export function* onAddPrefab(action) {
    try {
        const state = yield select((state) => state)
        console.log("DEBUG: " + state.objects.tile[state.interactionLevel.tileId].rack._id)
        const currentRackId = yield select((state) => state.objects.tile[state.interactionLevel.tileId].rack._id)
        const currentRackJson = yield call(getTopologyAsObject, currentRackId)
        const prefab = yield call(addPrefab, { name: action.name, rack: currentRackJson })
        yield put(addToStore('prefab', prefab))

    } catch (error) {
        console.error(error)
    }
}
