import {call, put} from "redux-saga/effects";
import {addToStore} from "../../actions/objects";
import {addSimulationSucceeded, deleteSimulationSucceeded} from "../../actions/simulations";
import {addSimulation, deleteSimulation} from "../routes/simulations";

export function* onSimulationAdd(action) {
    try {
        const simulation = yield call(addSimulation, {name: action.name});
        yield put(addToStore("simulation", simulation));

        const authorization = {
            simulationId: simulation.id,
            userId: action.userId,
            authorizationLevel: "OWN"
        };
        yield put(addToStore("authorization", authorization));
        yield put(addSimulationSucceeded([authorization.userId, authorization.simulationId]));
    } catch (error) {
        console.log(error);
    }
}

export function* onSimulationDelete(action) {
    try {
        yield call(deleteSimulation, action.id);
        yield put(deleteSimulationSucceeded(action.id));
    } catch (error) {
        console.log(error);
    }
}
