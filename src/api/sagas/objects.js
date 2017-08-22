import {call, put, select} from "redux-saga/effects";
import {addToStore} from "../../actions/objects";
import {getSimulation} from "../routes/simulations";
import {getUser} from "../routes/users";

const selectors = {
    simulation: state => state.objects.simulation,
    user: state => state.objects.user,
    authorization: state => state.objects.authorization,
};

function* fetchAndStoreObject(objectType, id, apiCall) {
    const objectStore = yield select(selectors[objectType]);
    if (!objectStore[id]) {
        const object = yield apiCall;
        yield put(addToStore(objectType, object));
    }
}

export const fetchAndStoreSimulation = (id) =>
    fetchAndStoreObject("simulation", id, call(getSimulation, id));

export const fetchAndStoreUser = (id) =>
    fetchAndStoreObject("user", id, call(getUser, id),);
