import { call, delay, put, select } from 'redux-saga/effects'
import { addPropToStoreObject, addToStore } from '../actions/objects'
import { setLastSimulatedTick } from '../actions/simulation/tick'
import { addBatchToStates } from '../actions/states'
import {
    deleteExperiment,
    getAllMachineStates,
    getAllRackStates,
    getAllRoomStates,
    getExperiment,
} from '../api/routes/experiments'
import { addExperiment, getSimulation } from '../api/routes/simulations'
import { fetchAndStoreAllSchedulers, fetchAndStoreAllTraces } from './objects'
import { fetchAndStoreAllTopologiesOfSimulation, fetchTopologyOfExperiment } from './topology'

export function* onOpenExperimentSucceeded(action) {
    try {
        const simulation = yield call(getSimulation, action.simulationId)
        yield put(addToStore('simulation', simulation))

        const experiment = yield call(getExperiment, action.experimentId)
        yield put(addToStore('experiment', experiment))

        yield fetchExperimentSpecifications()

        yield fetchTopologyOfExperiment(experiment)
        yield startStateFetchLoop(action.experimentId)
    } catch (error) {
        console.error(error)
    }
}

function* startStateFetchLoop(experimentId) {
    try {
        while ((yield select((state) => state.currentExperimentId)) !== '-1') {
            const lastSimulatedTick = (yield call(getExperiment, experimentId)).lastSimulatedTick
            if (lastSimulatedTick !== (yield select((state) => state.lastSimulatedTick))) {
                yield put(setLastSimulatedTick(lastSimulatedTick))

                const machineStates = yield call(getAllMachineStates, experimentId)
                const rackStates = yield call(getAllRackStates, experimentId)
                const roomStates = yield call(getAllRoomStates, experimentId)

                yield put(addBatchToStates('machine', machineStates))
                yield put(addBatchToStates('rack', rackStates))
                yield put(addBatchToStates('room', roomStates))

                yield delay(5000)
            } else {
                yield delay(10000)
            }
        }
    } catch (error) {
        console.error(error)
    }
}

export function* onFetchExperimentsOfSimulation() {
    try {
        const currentSimulationId = yield select((state) => state.currentSimulationId)
        const currentSimulation = yield select((state) => state.object.simulation[currentSimulationId])

        yield fetchExperimentSpecifications()

        for (let i in currentSimulation.experimentIds) {
            const experiment = yield call(getExperiment, currentSimulation.experimentIds[i])
            yield put(addToStore('experiment', experiment))
        }
    } catch (error) {
        console.error(error)
    }
}

function* fetchExperimentSpecifications() {
    try {
        const currentSimulationId = yield select((state) => state.currentSimulationId)
        yield fetchAndStoreAllTopologiesOfSimulation(currentSimulationId)
        yield fetchAndStoreAllTraces()
        yield fetchAndStoreAllSchedulers()
    } catch (error) {
        console.error(error)
    }
}

export function* onAddExperiment(action) {
    try {
        const currentSimulationId = yield select((state) => state.currentSimulationId)

        const experiment = yield call(
            addExperiment,
            currentSimulationId,
            Object.assign({}, action.experiment, {
                id: '-1',
                simulationId: currentSimulationId,
            })
        )
        yield put(addToStore('experiment', experiment))

        const experimentIds = yield select((state) => state.objects.simulation[currentSimulationId].experimentIds)
        yield put(
            addPropToStoreObject('simulation', currentSimulationId, {
                experimentIds: experimentIds.concat([experiment._id]),
            })
        )
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteExperiment(action) {
    try {
        yield call(deleteExperiment, action.id)

        const currentSimulationId = yield select((state) => state.currentSimulationId)
        const experimentIds = yield select((state) => state.objects.simulation[currentSimulationId].experimentIds)

        yield put(
            addPropToStoreObject('simulation', currentSimulationId, {
                experimentIds: experimentIds.filter((id) => id !== action.id),
            })
        )
    } catch (error) {
        console.error(error)
    }
}
