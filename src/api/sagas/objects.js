import {call, put, select} from "redux-saga/effects";
import {addToSimulationStore, addToUserStore} from "../../actions/objects";
import {getSimulation} from "../routes/simulations";
import {getUser} from "../routes/users";

const selectors = {
    simulation: state => state.objects.simulations,
    user: state => state.objects.users,
    authorization: state => state.objects.authorizations,
};

function* fetchAndStoreObject(objectType, id, apiCall, addToStore) {
    const objectStore = yield select(selectors[objectType]);
    if (!objectStore[id]) {
        const object = yield apiCall;
        yield put(addToStore(object));
    }
}

export const fetchAndStoreSimulation = (id) =>
    fetchAndStoreObject("simulation", id, call(getSimulation, id), addToSimulationStore);

export const fetchAndStoreUser = (id) =>
    fetchAndStoreObject("user", id, call(getUser, id), addToUserStore);
