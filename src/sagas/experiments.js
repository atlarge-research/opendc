import {call, put, select} from "redux-saga/effects";
import {addPropToStoreObject, addToStore} from "../actions/objects";
import {deleteExperiment} from "../api/routes/experiments";
import {addExperiment, getExperimentsOfSimulation} from "../api/routes/simulations";
import {fetchAndStoreAllSchedulers, fetchAndStoreAllTraces, fetchAndStorePathsOfSimulation} from "./objects";

export function* onFetchExperimentsOfSimulation() {
    try {
        const currentSimulationId = yield select(state => state.currentSimulationId);

        yield fetchExperimentSpecifications();
        const experiments = yield call(getExperimentsOfSimulation, currentSimulationId);
        for (let i in experiments) {
            yield put(addToStore("experiment", experiments[i]));
        }
        yield put(addPropToStoreObject("simulation", currentSimulationId,
            {experimentIds: experiments.map(experiment => experiment.id)}));
    } catch (error) {
        console.error(error);
    }
}

function* fetchExperimentSpecifications() {
    try {
        const currentSimulationId = yield select(state => state.currentSimulationId);
        yield fetchAndStorePathsOfSimulation(currentSimulationId);
        yield fetchAndStoreAllTraces();
        yield fetchAndStoreAllSchedulers();
    } catch (error) {
        console.error(error);
    }
}

export function* onAddExperiment(action) {
    try {
        const currentSimulationId = yield select(state => state.currentSimulationId);

        const experiment = yield call(addExperiment,
            currentSimulationId,
            Object.assign({}, action.experiment, {
                id: -1,
                simulationId: currentSimulationId
            })
        );
        yield put(addToStore("experiment", experiment));

        const experimentIds = yield select(state => state.objects.simulation[currentSimulationId].experimentIds);
        yield put(addPropToStoreObject("simulation", currentSimulationId,
            {experimentIds: experimentIds.concat([experiment.id])}));
    } catch (error) {
        console.error(error);
    }
}

export function* onDeleteExperiment(action) {
    try {
        yield call(deleteExperiment, action.id);

        const currentSimulationId = yield select(state => state.currentSimulationId);
        const experimentIds = yield select(state => state.objects.simulation[currentSimulationId].experimentIds);

        yield put(addPropToStoreObject("simulation", currentSimulationId,
            {experimentIds: experimentIds.filter(id => id !== action.id)}));
    } catch (error) {
        console.error(error);
    }
}
