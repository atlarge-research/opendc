import {call, put} from "redux-saga/effects";
import {logInSucceeded} from "../../actions/auth";
import {addToAuthorizationStore, addToSimulationStore, addToUserStore} from "../../actions/object-stores";
import {fetchAuthorizationsOfCurrentUserSucceeded} from "../../actions/users";
import {performTokenSignIn} from "../routes/auth";
import {getSimulation} from "../routes/simulations";
import {addUser, getAuthorizationsByUser, getUser} from "../routes/users";

export function* fetchLoggedInUser(action) {
    try {
        const tokenResponse = yield call(performTokenSignIn, action.payload.authToken);
        let userId = tokenResponse.userId;

        if (tokenResponse.isNewUser) {
            const newUser = yield call(addUser, action.payload);
            userId = newUser.id;
        }

        yield put(logInSucceeded(Object.assign({userId}, action.payload)));
    } catch (error) {
        console.log(error);
    }
}

export function* fetchAuthorizationsOfCurrentUser(action) {
    try {
        const authorizations = yield call(getAuthorizationsByUser, action.userId);

        for (const authorization of authorizations) {
            yield put(addToAuthorizationStore(authorization));

            const simulation = yield call(getSimulation, authorization.simulationId);
            yield put(addToSimulationStore(simulation));

            const user = yield call(getUser, authorization.userId);
            yield put(addToUserStore(user));
        }

        const authorizationIds = authorizations.map(authorization => (
            [authorization.userId, authorization.simulationId]
        ));

        yield put(fetchAuthorizationsOfCurrentUserSucceeded(authorizationIds));
    } catch (error) {
        console.log(error);
    }
}
