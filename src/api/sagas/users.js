import {call, put} from "redux-saga/effects";
import {logInSucceeded} from "../../actions/auth";
import {addToAuthorizationStore} from "../../actions/objects";
import {fetchAuthorizationsOfCurrentUserSucceeded} from "../../actions/users";
import {saveAuthLocalStorage} from "../../auth/index";
import {performTokenSignIn} from "../routes/auth";
import {addUser, getAuthorizationsByUser} from "../routes/users";
import {fetchAndStoreSimulation, fetchAndStoreUser} from "./objects";

export function* onFetchLoggedInUser(action) {
    try {
        const tokenResponse = yield call(performTokenSignIn, action.payload.authToken);
        let userId = tokenResponse.userId;

        if (tokenResponse.isNewUser) {
            saveAuthLocalStorage({authToken: action.payload.authToken});
            const newUser = yield call(addUser, action.payload);
            userId = newUser.id;
        }

        yield put(logInSucceeded(Object.assign({userId}, action.payload)));
    } catch (error) {
        console.log(error);
    }
}

export function* onFetchAuthorizationsOfCurrentUser(action) {
    try {
        const authorizations = yield call(getAuthorizationsByUser, action.userId);

        for (const authorization of authorizations) {
            yield put(addToAuthorizationStore(authorization));

            yield fetchAndStoreSimulation(authorization.simulationId);
            yield fetchAndStoreUser(authorization.userId);
        }

        const authorizationIds = authorizations.map(authorization => (
            [authorization.userId, authorization.simulationId]
        ));

        yield put(fetchAuthorizationsOfCurrentUserSucceeded(authorizationIds));
    } catch (error) {
        console.log(error);
    }
}
