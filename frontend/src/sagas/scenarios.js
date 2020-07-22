import { call, put, select } from 'redux-saga/effects'
import { addPropToStoreObject, addToStore } from '../actions/objects'
import { getProject } from '../api/routes/projects'
import { fetchAndStoreAllSchedulers, fetchAndStoreAllTraces } from './objects'
import { fetchAndStoreAllTopologiesOfProject } from './topology'
import { addScenario, deleteScenario, updateScenario } from '../api/routes/scenarios'
import { fetchPortfolioWithScenarios, watchForPortfolioResults } from './portfolios'

export function* onOpenScenarioSucceeded(action) {
    try {
        const project = yield call(getProject, action.projectId)
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
        const scenario = yield call(addScenario, action.scenario.portfolioId, action.scenario)
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
        const scenario = yield call(updateScenario, action.scenario._id, action.scenario)
        yield put(addToStore('scenario', scenario))
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteScenario(action) {
    try {
        yield call(deleteScenario, action.id)

        const currentPortfolioId = yield select((state) => state.currentPortfolioId)
        const scenarioIds = yield select((state) => state.objects.project[currentPortfolioId].scenarioIds)

        yield put(
            addPropToStoreObject('scenario', currentPortfolioId, {
                scenarioIds: scenarioIds.filter((id) => id !== action.id),
            })
        )
    } catch (error) {
        console.error(error)
    }
}
