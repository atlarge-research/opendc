import { call, put, select, delay, getContext } from 'redux-saga/effects'
import { addPropToStoreObject, addToStore } from '../actions/objects'
import { addPortfolio, deletePortfolio, getPortfolio, updatePortfolio } from '../../api/portfolios'
import { getProject } from '../../api/projects'
import { fetchAndStoreAllSchedulers, fetchAndStoreAllTraces } from './objects'
import { fetchAndStoreAllTopologiesOfProject } from './topology'
import { getScenario } from '../../api/scenarios'

export function* onOpenPortfolioSucceeded(action) {
    try {
        const auth = yield getContext('auth')
        const project = yield call(getProject, auth, action.projectId)
        yield put(addToStore('project', project))
        yield fetchAndStoreAllTopologiesOfProject(project._id)
        yield fetchPortfoliosOfProject()
        yield fetchAndStoreAllSchedulers()
        yield fetchAndStoreAllTraces()

        yield watchForPortfolioResults()
    } catch (error) {
        console.error(error)
    }
}

export function* watchForPortfolioResults() {
    try {
        const currentPortfolioId = yield select((state) => state.currentPortfolioId)
        let unfinishedScenarios = yield getCurrentUnfinishedScenarios()

        while (unfinishedScenarios.length > 0) {
            yield delay(3000)
            yield fetchPortfolioWithScenarios(currentPortfolioId)
            unfinishedScenarios = yield getCurrentUnfinishedScenarios()
        }
    } catch (error) {
        console.error(error)
    }
}

export function* getCurrentUnfinishedScenarios() {
    try {
        const currentPortfolioId = yield select((state) => state.currentPortfolioId)
        const scenarioIds = yield select((state) => state.objects.portfolio[currentPortfolioId].scenarioIds)
        const scenarioObjects = yield select((state) => state.objects.scenario)
        const scenarios = scenarioIds.map((s) => scenarioObjects[s])
        return scenarios.filter((s) => !s || s.simulation.state === 'QUEUED' || s.simulation.state === 'RUNNING')
    } catch (error) {
        console.error(error)
    }
}

export function* fetchPortfoliosOfProject() {
    try {
        const currentProjectId = yield select((state) => state.currentProjectId)
        const currentProject = yield select((state) => state.objects.project[currentProjectId])

        yield fetchAndStoreAllSchedulers()
        yield fetchAndStoreAllTraces()

        for (let i in currentProject.portfolioIds) {
            yield fetchPortfolioWithScenarios(currentProject.portfolioIds[i])
        }
    } catch (error) {
        console.error(error)
    }
}

export function* fetchPortfolioWithScenarios(portfolioId) {
    try {
        const auth = yield getContext('auth')
        const portfolio = yield call(getPortfolio, auth, portfolioId)
        yield put(addToStore('portfolio', portfolio))

        for (let i in portfolio.scenarioIds) {
            const scenario = yield call(getScenario, auth, portfolio.scenarioIds[i])
            yield put(addToStore('scenario', scenario))
        }
        return portfolio
    } catch (error) {
        console.error(error)
    }
}

export function* onAddPortfolio(action) {
    try {
        const currentProjectId = yield select((state) => state.currentProjectId)
        const auth = yield getContext('auth')
        const portfolio = yield call(
            addPortfolio,
            auth,
            currentProjectId,
            Object.assign({}, action.portfolio, {
                projectId: currentProjectId,
                scenarioIds: [],
            })
        )
        yield put(addToStore('portfolio', portfolio))

        const portfolioIds = yield select((state) => state.objects.project[currentProjectId].portfolioIds)
        yield put(
            addPropToStoreObject('project', currentProjectId, {
                portfolioIds: portfolioIds.concat([portfolio._id]),
            })
        )
    } catch (error) {
        console.error(error)
    }
}

export function* onUpdatePortfolio(action) {
    try {
        const auth = yield getContext('auth')
        const portfolio = yield call(updatePortfolio, auth, action.portfolio._id, action.portfolio)
        yield put(addToStore('portfolio', portfolio))
    } catch (error) {
        console.error(error)
    }
}

export function* onDeletePortfolio(action) {
    try {
        const auth = yield getContext('auth')
        yield call(deletePortfolio, auth, action.id)

        const currentProjectId = yield select((state) => state.currentProjectId)
        const portfolioIds = yield select((state) => state.objects.project[currentProjectId].portfolioIds)

        yield put(
            addPropToStoreObject('project', currentProjectId, {
                portfolioIds: portfolioIds.filter((id) => id !== action.id),
            })
        )
    } catch (error) {
        console.error(error)
    }
}
