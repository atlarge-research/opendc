import { call, delay, put, select } from 'redux-saga/effects'
import { addPropToStoreObject, addToStore } from '../actions/objects'
import { setLastSimulatedTick } from '../actions/simulation/tick'
import { addBatchToStates } from '../actions/states'
import {
    deleteExperiment,
    getAllMachineStates,
    getAllRackStates,
    getAllRoomStates,
    getAllTaskStates,
    getExperiment,
    getLastSimulatedTick,
} from '../api/routes/experiments'
import { getTasksOfJob } from '../api/routes/jobs'
import { addExperiment, getExperimentsOfSimulation, getSimulation } from '../api/routes/simulations'
import { getJobsOfTrace } from '../api/routes/traces'
import { fetchAndStoreAllSchedulers, fetchAndStoreAllTraces, fetchAndStorePathsOfSimulation } from './objects'
import { fetchAllDatacentersOfExperiment } from './topology'

export function* onOpenExperimentSucceeded(action) {
    try {
        const simulation = yield call(getSimulation, action.simulationId)
        yield put(addToStore('simulation', simulation))

        const experiment = yield call(getExperiment, action.experimentId)
        yield put(addToStore('experiment', experiment))

        yield fetchExperimentSpecifications()
        yield fetchWorkloadOfTrace(experiment.traceId)

        yield fetchAllDatacentersOfExperiment(experiment)
        yield startStateFetchLoop(action.experimentId)
    } catch (error) {
        console.error(error)
    }
}

function* startStateFetchLoop(experimentId) {
    try {
        while ((yield select(state => state.currentExperimentId)) !== -1) {
            const lastSimulatedTick = (yield call(getLastSimulatedTick, experimentId))
                .lastSimulatedTick
            if (
                lastSimulatedTick !== (yield select(state => state.lastSimulatedTick))
            ) {
                yield put(setLastSimulatedTick(lastSimulatedTick))

                const taskStates = yield call(getAllTaskStates, experimentId)
                const machineStates = yield call(getAllMachineStates, experimentId)
                const rackStates = yield call(getAllRackStates, experimentId)
                const roomStates = yield call(getAllRoomStates, experimentId)

                yield put(addBatchToStates('task', taskStates))
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
        const currentSimulationId = yield select(
            state => state.currentSimulationId,
        )

        yield fetchExperimentSpecifications()
        const experiments = yield call(
            getExperimentsOfSimulation,
            currentSimulationId,
        )
        for (let i in experiments) {
            yield put(addToStore('experiment', experiments[i]))
        }
        yield put(
            addPropToStoreObject('simulation', currentSimulationId, {
                experimentIds: experiments.map(experiment => experiment.id),
            }),
        )
    } catch (error) {
        console.error(error)
    }
}

function* fetchExperimentSpecifications() {
    try {
        const currentSimulationId = yield select(
            state => state.currentSimulationId,
        )
        yield fetchAndStorePathsOfSimulation(currentSimulationId)
        yield fetchAndStoreAllTraces()
        yield fetchAndStoreAllSchedulers()
    } catch (error) {
        console.error(error)
    }
}

function* fetchWorkloadOfTrace(traceId) {
    try {
        const jobs = yield call(getJobsOfTrace, traceId)
        for (let i in jobs) {
            const job = jobs[i]
            const tasks = yield call(getTasksOfJob, job.id)
            job.taskIds = tasks.map(task => task.id)
            for (let j in tasks) {
                yield put(addToStore('task', tasks[j]))
            }
            yield put(addToStore('job', job))
        }
        yield put(
            addPropToStoreObject('trace', traceId, {
                jobIds: jobs.map(job => job.id),
            }),
        )
    } catch (error) {
        console.error(error)
    }
}

export function* onAddExperiment(action) {
    try {
        const currentSimulationId = yield select(
            state => state.currentSimulationId,
        )

        const experiment = yield call(
            addExperiment,
            currentSimulationId,
            Object.assign({}, action.experiment, {
                id: -1,
                simulationId: currentSimulationId,
            }),
        )
        yield put(addToStore('experiment', experiment))

        const experimentIds = yield select(
            state => state.objects.simulation[currentSimulationId].experimentIds,
        )
        yield put(
            addPropToStoreObject('simulation', currentSimulationId, {
                experimentIds: experimentIds.concat([experiment.id]),
            }),
        )
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteExperiment(action) {
    try {
        yield call(deleteExperiment, action.id)

        const currentSimulationId = yield select(
            state => state.currentSimulationId,
        )
        const experimentIds = yield select(
            state => state.objects.simulation[currentSimulationId].experimentIds,
        )

        yield put(
            addPropToStoreObject('simulation', currentSimulationId, {
                experimentIds: experimentIds.filter(id => id !== action.id),
            }),
        )
    } catch (error) {
        console.error(error)
    }
}
