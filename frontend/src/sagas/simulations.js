import { call, put } from 'redux-saga/effects'
import { addToStore } from '../actions/objects'
import { addSimulationSucceeded, deleteSimulationSucceeded } from '../actions/simulations'
import { addSimulation, deleteSimulation, getSimulation } from '../api/routes/simulations'
import { fetchLatestDatacenter } from './topology'

export function* onOpenSimulationSucceeded(action) {
    try {
        const simulation = yield call(getSimulation, action.id)
        yield put(addToStore('simulation', simulation))

        yield fetchLatestDatacenter(action.id)
    } catch (error) {
        console.error(error)
    }
}

export function* onSimulationAdd(action) {
    try {
        const simulation = yield call(addSimulation, { name: action.name })
        yield put(addToStore('simulation', simulation))

        const authorization = {
            simulationId: simulation._id,
            userId: action.userId,
            authorizationLevel: 'OWN',
            simulation,
        }
        yield put(addToStore('authorization', authorization))
        yield put(
            addSimulationSucceeded([authorization.userId, authorization.simulationId]),
        )
    } catch (error) {
        console.error(error)
    }
}

export function* onSimulationDelete(action) {
    try {
        yield call(deleteSimulation, action.id)
        yield put(deleteSimulationSucceeded(action.id))
    } catch (error) {
        console.error(error)
    }
}
