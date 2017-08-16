import {takeEvery} from "redux-saga/effects";
import {LOG_IN} from "../../actions/auth";
import {ADD_SIMULATION} from "../../actions/simulations";
import {FETCH_AUTHORIZATIONS_OF_CURRENT_USER} from "../../actions/users";
import {onSimulationAdd} from "./simulations";
import {onFetchAuthorizationsOfCurrentUser, onFetchLoggedInUser} from "./users";

export default function* rootSaga() {
    yield takeEvery(LOG_IN, onFetchLoggedInUser);
    yield takeEvery(FETCH_AUTHORIZATIONS_OF_CURRENT_USER, onFetchAuthorizationsOfCurrentUser);
    yield takeEvery(ADD_SIMULATION, onSimulationAdd);
}
