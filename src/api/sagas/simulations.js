import {call, put} from "redux-saga/effects";
import {addToAuthorizationStore, addToSimulationStore} from "../../actions/objects";
import {addSimulationSucceeded} from "../../actions/simulations";
import {addSimulation} from "../routes/simulations";

export function* onSimulationAdd(action) {
    try {
        const simulation = yield call(addSimulation, {name: action.name});
        yield put(addToSimulationStore(simulation));

        const authorization = {
            simulationId: simulation.id,
            userId: action.userId,
            authorizationLevel: "OWN"
        };
        yield put(addToAuthorizationStore(authorization));
        yield put(addSimulationSucceeded([authorization.userId, authorization.simulationId]));
    } catch (error) {
        console.log(error);
    }
}
