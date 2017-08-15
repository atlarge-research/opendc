import {takeEvery} from "redux-saga/effects";
import {LOG_IN} from "../../actions/auth";
import {FETCH_AUTHORIZATIONS_OF_CURRENT_USER} from "../../actions/users";
import {fetchAuthorizationsOfCurrentUser, fetchLoggedInUser} from "./users";

export default function* rootSaga() {
    yield takeEvery(LOG_IN, fetchLoggedInUser);
    yield takeEvery(FETCH_AUTHORIZATIONS_OF_CURRENT_USER, fetchAuthorizationsOfCurrentUser);
}
