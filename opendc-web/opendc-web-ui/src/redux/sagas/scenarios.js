import { call, put, select, getContext } from 'redux-saga/effects'
import { addPropToStoreObject, addToStore } from '../actions/objects'
import { getProject } from '../../api/projects'
import { fetchAndStoreAllSchedulers, fetchAndStoreAllTraces } from './objects'
import { fetchAndStoreAllTopologiesOfProject } from './topology'
import { addScenario, deleteScenario, updateScenario } from '../../api/scenarios'
import { fetchPortfolioWithScenarios, watchForPortfolioResults } from './portfolios'

export function* onOpenScenarioSucceeded(action) {
    try {
        const auth = yield getContext('auth')
        const project = yield call(getProject, auth, action.projectId)
        yield put(addToStore('project', project))
        yield fetchAndStoreAllTopologiesOfProject(project._id)
        yield fetchAndStoreAllSchedulers()
        yield fetchAndStoreAllTraces()
        yield fetchPortfolioWithScenarios(action.portfolioId)

        // TODO Fetch scenario-specific metrics
    } catch (error) {
        console.error(error)
    }
}

export function* onAddScenario(action) {
    try {
        const auth = yield getContext('auth')
        const scenario = yield call(addScenario, auth, action.scenario.portfolioId, action.scenario)
        yield put(addToStore('scenario', scenario))

        const scenarioIds = yield select((state) => state.objects.portfolio[action.scenario.portfolioId].scenarioIds)
        yield put(
            addPropToStoreObject('portfolio', action.scenario.portfolioId, {
                scenarioIds: scenarioIds.concat([scenario._id]),
            })
        )
        yield watchForPortfolioResults()
    } catch (error) {
        console.error(error)
    }
}

export function* onUpdateScenario(action) {
    try {
        const auth = yield getContext('auth')
        const scenario = yield call(updateScenario, auth, action.scenario._id, action.scenario)
        yield put(addToStore('scenario', scenario))
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteScenario(action) {
    try {
        const auth = yield getContext('auth')
        yield call(deleteScenario, auth, action.id)

        const currentPortfolioId = yield select((state) => state.currentPortfolioId)
        const scenarioIds = yield select((state) => state.objects.portfolio[currentPortfolioId].scenarioIds)

        yield put(
            addPropToStoreObject('scenario', currentPortfolioId, {
                scenarioIds: scenarioIds.filter((id) => id !== action.id),
            })
        )
    } catch (error) {
        console.error(error)
    }
}
