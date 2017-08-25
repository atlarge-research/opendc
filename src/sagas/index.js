import {takeEvery} from "redux-saga/effects";
import {LOG_IN} from "../actions/auth";
import {ADD_SIMULATION, DELETE_SIMULATION} from "../actions/simulations";
import {FETCH_LATEST_DATACENTER} from "../actions/topology";
import {DELETE_CURRENT_USER, FETCH_AUTHORIZATIONS_OF_CURRENT_USER} from "../actions/users";
import {onDeleteCurrentUser} from "./profile";
import {onSimulationAdd, onSimulationDelete} from "./simulations";
import {onFetchLatestDatacenter} from "./topology";
import {onFetchAuthorizationsOfCurrentUser, onFetchLoggedInUser} from "./users";

export default function* rootSaga() {
    yield takeEvery(LOG_IN, onFetchLoggedInUser);
    yield takeEvery(FETCH_AUTHORIZATIONS_OF_CURRENT_USER, onFetchAuthorizationsOfCurrentUser);
    yield takeEvery(ADD_SIMULATION, onSimulationAdd);
    yield takeEvery(DELETE_SIMULATION, onSimulationDelete);
    yield takeEvery(DELETE_CURRENT_USER, onDeleteCurrentUser);
    yield takeEvery(FETCH_LATEST_DATACENTER, onFetchLatestDatacenter);
}
